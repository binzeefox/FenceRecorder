package com.cloud_hermits.fencerecorder.cat;

import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.binzeefox.foxframe.core.FoxCore;
import com.binzeefox.foxframe.tools.dev.ThreadUtil;
import com.binzeefox.foxframe.tools.phone.NoticeUtil;
import com.cloud_hermits.fencerecorder.R;
import com.cloud_hermits.fencerecorder.base.BaseActivity;
import com.cloud_hermits.fencerecorder.base.BaseApplication;
import com.cloud_hermits.fencerecorder.database.tables.Match;
import com.cloud_hermits.fencerecorder.recorder_core.ConfigUtil;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import butterknife.BindView;
import butterknife.OnClick;

import static com.cloud_hermits.fencerecorder.recorder_core.MatchRecorder.DB_NAME;

/**
 * 设置页
 * @author binze
 * 2020/6/8 17:01
 */
public class ConfigActivity extends BaseActivity {
    private static final String TAG = "ConfigActivity";

    @BindView(R.id.period_minute)
    EditText minuteField;
    @BindView(R.id.period_second)
    EditText secondField;

    @Override
    protected int onSetLayoutResource() {
        return R.layout.activity_match_config;
    }

    @Override
    protected void create(Bundle savedInstanceState) {
        super.create(savedInstanceState);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("比赛设置");
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        initData();
    }

    /**
     * 初始化数据
     * @author binze 2020/6/9 9:47
     */
    private void initData() {
        long period = ConfigUtil.getMatchPeriod();
        minuteField.setText(new SimpleDateFormat("m", Locale.CHINA).format(period));
        secondField.setText(new SimpleDateFormat("ss", Locale.CHINA).format(period));
    }

    private void saveConfig() {
        int minute = getViewHelper().getIntegerById(R.id.period_minute);
        int second = getViewHelper().getIntegerById(R.id.period_second);

        long period = minute * 60 * 1000 + second * 1000;
        ConfigUtil.setMatchPeriod(period);
        NoticeUtil.get().showToast("保存成功");
    }

    @OnClick(R.id.clear_data)
    void clickReset(){
        new AlertDialog.Builder(this)
                .setTitle("警告")
                .setMessage("该操作将清除所有记录，是否继续？")
                .setCancelable(true)
                .setNegativeButton("取消", null)
                .setPositiveButton("清除", (dialog, which) -> ThreadUtil.get().execute(() -> {
                    if (BaseApplication.clearMatchTable()) {
                        FoxCore.get().putGlobalData(GLOBAL_KEY_MATCHES, new ArrayList<Match>());
                        resetResult(true);
                    } else resetResult(false);
                }))
                .show();
    }

    /**
     * 清除是否成功
     * @author binze 2020/6/9 11:19
     */
    private void resetResult(boolean success){
        runOnUiThread(() -> {
            if (success) NoticeUtil.get().showToast("清除数据成功");
            else NoticeUtil.get().showToast("未知错误");
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_save) saveConfig();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_match_config, menu);
        return super.onCreateOptionsMenu(menu);
    }
}
