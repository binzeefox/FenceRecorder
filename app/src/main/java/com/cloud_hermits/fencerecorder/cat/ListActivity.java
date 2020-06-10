package com.cloud_hermits.fencerecorder.cat;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.binzeefox.foxframe.core.FoxCore;
import com.binzeefox.foxframe.core.tools.DataHolder;
import com.binzeefox.foxframe.tools.dev.ThreadUtil;
import com.binzeefox.foxframe.tools.phone.NoticeUtil;
import com.cloud_hermits.fencerecorder.R;
import com.cloud_hermits.fencerecorder.base.BaseActivity;
import com.cloud_hermits.fencerecorder.base.BaseApplication;
import com.cloud_hermits.fencerecorder.database.tables.Match;
import com.tencent.bugly.beta.Beta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.widget.Toolbar;
import butterknife.BindView;
import butterknife.OnClick;

/**
 * 主页面
 *
 * @author binze 2020/6/8 15:28
 */
public class ListActivity extends BaseActivity {
    private static final String TAG = "ListActivity";
    private final String SUBMIT_KEY = getClass().getName() + new Date().getTime();

    @BindView(R.id.list_main)
    ListView mListView;

    private List<Match> mData = new ArrayList<>();  //保存的数据
    private ListAdapter mAdapter = new ListAdapter();   //列表适配器

    @Override
    protected int onSetLayoutResource() {
        return R.layout.activity_list;
    }

    @Override
    protected void create(Bundle savedInstanceState) {
        super.create(savedInstanceState);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this::onItemClick);
        mListView.setOnItemLongClickListener(this::onHoldItem);
        mData = FoxCore.get().getGlobalData(GLOBAL_KEY_MATCHES, new ArrayList<Match>());
        FoxCore.get().submitGlobalData(SUBMIT_KEY, this::onDataChanged);

        Beta.checkUpgrade(false, false);    //检查更新
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FoxCore.get().unSubmitGlobalData(SUBMIT_KEY);
    }

    /**
     * 数据变化
     *
     * @author binze 2020/6/8 15:48
     */
    private void onDataChanged(String key, Object value) {
        runOnUiThread(() -> {
            if (!TextUtils.equals(GLOBAL_KEY_MATCHES, key)) return;
//        mData.clear();
//        mData.addAll((List<Match>) value);
            if (value == null) mData = new ArrayList<>();
            else mData = (List<Match>) value;
            mAdapter.notifyDataSetChanged();
        });

    }

    /**
     * 点击事件
     *
     * @author binze 2020/6/8 16:56
     */
    @OnClick({R.id.fab_add})
    void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab_add:
                navigate(MainActivity.class).commit();
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (checkCallAgain(2000))
            System.exit(0);
        else NoticeUtil.get().showToast("再次点击返回键退出App");
    }

    /**
     * 菜单点击事件
     *
     * @author binze 2019/11/21 12:04
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {   //比赛设置
            navigate(ConfigActivity.class).commit();
            return true;
        }

        if (id == R.id.about) { //关于页/宣传页
            navigate(AboutActivity.class).commit();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * 点击子项
     *
     * @author binze 2020/6/8 15:42
     */
    private void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        final Match match = mData.get(position);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_match_detail, null);
        TextView periodField, dateField, redField, blueField, redScoreField, blueScoreField;
        final EditText commentField;
        periodField = dialogView.findViewById(R.id.period_field);
        dateField = dialogView.findViewById(R.id.date_field);
        redField = dialogView.findViewById(R.id.red_name_field);
        blueField = dialogView.findViewById(R.id.blue_name_field);
        redScoreField = dialogView.findViewById(R.id.red_score_field);
        blueScoreField = dialogView.findViewById(R.id.blue_score_field);
        commentField = dialogView.findViewById(R.id.comment_field);
        periodField.setText(new SimpleDateFormat("用时 mm:ss", Locale.CHINA)
                .format(match.getPeriod()));
        dateField.setText(new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.CHINA)
                .format(match.getTimestamp()));
        redField.setText(match.getRedName());
        blueField.setText(match.getBlueName());
        redScoreField.setText(String.format(Locale.CHINA, "%d", match.getRedScore()));
        blueScoreField.setText(String.format(Locale.CHINA, "%d", match.getBlueScore()));
        commentField.setText(match.getComment());

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .setPositiveButton("更新备注", (dialog, which) -> {
                    match.setComment(commentField.getText().toString());
                    ThreadUtil.get().execute(() -> {
                        BaseApplication.getDatabase().selectMatch().update(match);
//                        FoxCore.get().removeGlobalData(GLOBAL_KEY_MATCHES);
                        FoxCore.get().putGlobalData(GLOBAL_KEY_MATCHES, mData);
                    });
                }).show();
    }

    /**
     * 长按子项
     *
     * @author binze 2020/6/8 15:43
     */
    private boolean onHoldItem(AdapterView<?> adapterView, View view, int position, long id) {
//        Match match = mData.get(position);
        PopupMenu menu = new PopupMenu(this, view, Gravity.END | Gravity.TOP);
        menu.getMenu().add(R.id.menu_group_list_popup, R.id.menu_delete, 0, "删除");
        menu.setOnMenuItemClickListener(item -> {
            onDeleteItem(position);
            menu.dismiss();
            return true;
        });
        menu.show();
        return true;
    }

    /**
     * 删除条目
     *
     * @author binze 2020/6/8 16:55
     */
    private void onDeleteItem(int position) {
        final Match match = mData.get(position);
        ThreadUtil.get().execute(() -> {
            BaseApplication.getDatabase().selectMatch().delete(match);
            mData.remove(position);
            FoxCore.get().putGlobalData(GLOBAL_KEY_MATCHES, mData);
        });
    }


    /**
     * 列表适配器
     *
     * @author binze 2019/12/16 11:18
     */
    private class ListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);

            Match match = mData.get(position);

            TextView text1 = convertView.findViewById(android.R.id.text1);
            TextView text2 = convertView.findViewById(android.R.id.text2);

            String title, date;
            title = String.format(Locale.CHINA, "%s vs %s 比分 %d : %d", match.getRedName()
                    , match.getBlueName(), match.getRedScore(), match.getBlueScore());
            date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date(match.getTimestamp()));

            text1.setText(title);
            text2.setText(date);
            return convertView;
        }
    }
}
