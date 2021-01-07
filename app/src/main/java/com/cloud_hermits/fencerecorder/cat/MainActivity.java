package com.cloud_hermits.fencerecorder.cat;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.binzeefox.foxframe.core.FoxCore;
import com.binzeefox.foxframe.core.base.FoxActivity;
import com.binzeefox.foxframe.tools.dev.TextTools;
import com.binzeefox.foxframe.tools.dev.ThreadUtil;
import com.binzeefox.foxframe.tools.phone.NoticeUtil;
import com.cloud_hermits.fencerecorder.R;
import com.cloud_hermits.fencerecorder.base.BaseActivity;
import com.cloud_hermits.fencerecorder.base.BaseApplication;
import com.cloud_hermits.fencerecorder.database.tables.Match;
import com.cloud_hermits.fencerecorder.recorder_core.ConfigUtil;
import com.cloud_hermits.fencerecorder.recorder_core.MatchRecorder;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.widget.Toolbar;
import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * 主页
 * <p>
 * · 开始计时后比赛开始，此时无法更改双方选手名称
 * · 为防止错误操作，只有计时器暂停时才能修改分数
 * · 计时暂停的状态下，为防止错误操作，需要2秒内两次点击返回键才能终止比赛
 * · 比赛终止后，自动保存数据到数据库，再次点击返回键则返回列表
 *
 * @author binze
 * 2020/6/8 17:03
 */
public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";
    private MatchRecorder recorder; //记录器
    private long period = ConfigUtil.getMatchPeriod();
    private boolean finish = false; //是否完成

    @BindView(R.id.timer_field)
    TextView timerField;
    @BindView(R.id.red_side_field)
    EditText redSideField;
    @BindView(R.id.blue_side_field)
    EditText blueSideField;
    @BindView(R.id.red_score_field)
    TextView redScoreField;
    @BindView(R.id.blue_score_field)
    TextView blueScoreField;
    @BindView(R.id.fab_icon)
    ImageView fabIcon;

    @Override
    protected int onSetLayoutResource() {
        return R.layout.activity_main;
    }

    @Override
    protected void create(Bundle savedInstanceState) {
        super.create(savedInstanceState);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("");
        timerField.setText(getTimeByLong(period));
        timerField.setOnClickListener(v -> {
            if (recorder != null) return;
            View layout = getLayoutInflater().inflate(R.layout.dialog_main_period_setter, null, false);
            EditText minField = layout.findViewById(R.id.period_minute);
            EditText secField = layout.findViewById(R.id.period_second);
            minField.setText(new SimpleDateFormat("m", Locale.CHINA).format(period));
            secField.setText(new SimpleDateFormat("ss", Locale.CHINA).format(period));
            new AlertDialog.Builder(this)
                    .setTitle("请输入本局比赛时长")
                    .setCancelable(true)
                    .setView(layout)
                    .setPositiveButton("确定", (dialog, which) -> {
                        String min, sec;
                        min = minField.getText().toString();
                        sec = secField.getText().toString();
                        if (min.isEmpty()){
                            minField.setError("该项不能为空");
                            return;
                        }
                        if (sec.isEmpty()){
                            secField.setError("该项不能为空");
                            return;
                        }
                        if (!TextTools.isInteger(min)) {
                            minField.setError("请输入自然数");
                            return;
                        }
                        if (!TextTools.isInteger(sec)){
                            secField.setError("请输入自然数");
                            return;
                        }
                        int minute = Integer.parseInt(min);
                        int second = Integer.parseInt(sec);
                        period = minute * 60 * 1000 + second * 1000;
                        timerField.setText(getTimeByLong(period));
                    }).show();
        });
    }

    /**
     * 菜单点击事件
     *
     * @author binze 2019/11/21 12:04
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.about) { //说明
            if (recorder == null || !recorder.isRunning()) {
                //显示说明弹窗
                new AlertDialog.Builder(this)
                        .setTitle("使用说明")
                        .setCancelable(true)
                        .setMessage(getIntro())
                        .show();
                return true;
            } else if (recorder.isRunning()){
                NoticeUtil.get().showToast("计时状态下屏蔽按键");
                return false;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 获取使用说明
     *
     * @author binze 2020/6/19 11:27
     */
    private String getIntro() {
        return "1. 输入双方选手姓名\n" +
                "2. （若需要）点击比赛时长修改比赛时长\n" +
                "3. 点击开始按钮进行计时\n" +
                "4. 若出现得分，暂停计时并修改分数\n" +
                "5. 时间到，裁判修改最终分数并按返回键退出\n" +
                "6. 若提前结束，需先暂停计时，并按两次返回键结束比赛，按第三次返回键退出\n\n" +
                "P.S. \n为防止误触，计时状态下将屏蔽除暂停外所有操作。如需修改分数，请先暂停计时。\n比赛开始后将不能修改双方选手名称和比赛时长。\n比赛结束后可在列表中添加备注。";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.action_settings).setVisible(false);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (recorder != null) {
            if (recorder.isRunning()) {
                //运行中
                NoticeUtil.get().showToast("计时状态下屏蔽返回键");
            } else if (!finish) {
                //比赛没有结束
                if (!checkCallAgain(2000)) {
                    NoticeUtil.get().showToast("再次点击返回键结束并记录比赛");
                } else {
                    recorder.stop();
                    endMatch();
                }
            } else {
                //若比赛结束，则返回列表。此时recorder.isRunning() 为false，finish为true，所有的输入框和按钮全部禁用
                super.onBackPressed();
            }
        } else super.onBackPressed();
    }

    /**
     * 比赛结束
     *
     * @author binze 2020/6/8 17:14
     */
    private void endMatch() {
        NoticeUtil.get().showToast("比赛结束，再次点击返回键返回列表");
        finish = true;
        getViewHelper().setViewsEnable(false, redScoreField, blueScoreField);
        getViewHelper().setViewsEnableById(false, R.id.fab_action, R.id.btn_plus_red
                , R.id.btn_plus_blue, R.id.btn_sub_red, R.id.btn_sub_blue);
    }

    /**
     * 通过long获取剩余时间
     *
     * @author binze 2019/12/17 11:35
     */
    private String getTimeByLong(long period) {
        return new SimpleDateFormat("mm:ss", Locale.getDefault()).format(period);
    }

    /**
     * 点击事件控制器
     *
     * @author binze 2019/12/17 11:17
     */
    @OnClick({R.id.fab_action, R.id.btn_plus_red, R.id.btn_plus_blue
            , R.id.btn_sub_red, R.id.btn_sub_blue})
    void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab_action:
                actionClicked();
                return;
            case R.id.btn_plus_red:
                changeScore(0, 1);
                return;
            case R.id.btn_plus_blue:
                changeScore(1, 1);
                return;
            case R.id.btn_sub_red:
                changeScore(0, -1);
                return;
            case R.id.btn_sub_blue:
                changeScore(1, -1);
                return;
            default:
                break;
        }
    }

    /**
     * 点击动作键
     *
     * @author binze 2020/6/8 17:15
     */
    private void actionClicked() {
        if (recorder == null) {
            getViewHelper().setViewsEnable(false, redSideField, blueSideField);
            redSideField.clearFocus();
            blueScoreField.clearFocus();
            String redSide = redSideField.getText().toString();
            String blueSide = blueSideField.getText().toString();
            if (TextUtils.isEmpty(redSide)) {
                redSide = "红方";
                redScoreField.setText(redSide);
            }
            if (TextUtils.isEmpty(blueSide)) {
                blueSide = "蓝方";
                blueScoreField.setText(blueSide);
            }
            recorder = new MatchRecorder(period, blueSide, redSide);
            recorder.observable().subscribe(new Observer<Long>() {
                @Override
                public void onSubscribe(Disposable d) {
                    dContainer.add(d);
                }

                @Override
                public void onNext(Long aLong) {
                    timerField.setText(getTimeByLong(aLong));
                }

                @Override
                public void onError(Throwable e) {
                    Log.e(TAG, "onError: 计时器出错", e);
                }

                @Override
                public void onComplete() {
                    fabIcon.setImageResource(R.drawable.ic_complete);
                    recorder.pause();
                    getViewHelper().setViewsEnableById(true, R.id.btn_plus_red, R.id.btn_plus_blue
                            , R.id.btn_sub_red, R.id.btn_sub_blue);
                    getViewHelper().setViewsEnableById(false, R.id.fab_action);
                }
            });
        }
        if (recorder.isRunning()) {
            recorder.pause();
            fabIcon.setImageResource(R.drawable.ic_play);
            getViewHelper().setViewsEnableById(true, R.id.btn_plus_red, R.id.btn_plus_blue
                    , R.id.btn_sub_red, R.id.btn_sub_blue);
        } else {
            recorder.start();
            fabIcon.setImageResource(R.drawable.ic_pause);
            getViewHelper().setViewsEnableById(false, R.id.btn_plus_red, R.id.btn_plus_blue
                    , R.id.btn_sub_red, R.id.btn_sub_blue);
        }
    }

    /**
     * 改变分数
     *
     * @param target 目标 0为红，1为蓝
     * @param score  分数
     * @author binze 2019/12/17 11:43
     */
    private void changeScore(int target, int score) {
        if (recorder == null) {
            NoticeUtil.get().showToast("比赛尚未开始");
            return;
        }
        String i = String.format("s%s", 112.3333);
        switch (target) {
            case 0:
                recorder.scoreRed(score);
                break;
            case 1:
                recorder.scoreBlue(score);
                break;
        }
        int[] recordScore = recorder.getScore();
        redScoreField.setText(String.format(Locale.CHINA, "%02d", recordScore[0]));
        blueScoreField.setText(String.format(Locale.CHINA, "%02d", recordScore[1]));
    }
}