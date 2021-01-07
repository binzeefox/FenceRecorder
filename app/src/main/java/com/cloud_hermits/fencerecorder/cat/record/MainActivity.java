package com.cloud_hermits.fencerecorder.cat.record;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.binzeefox.foxframe.tools.dev.TextTools;
import com.binzeefox.foxframe.tools.dev.ThreadUtil;
import com.binzeefox.foxframe.tools.phone.NoticeUtil;
import com.cloud_hermits.fencerecorder.R;
import com.cloud_hermits.fencerecorder.base.BaseActivity;
import com.cloud_hermits.fencerecorder.recorder_core.ConfigUtil;
import com.cloud_hermits.fencerecorder.recorder_core.MatchRecorder;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Locale;

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
 * · 比赛终止后，自动保存数据到数据库，同时若绑定了传感器设备，则一同保存传感器数据。然后尝试上传至服务器。再次点击返回键则返回列表
 * @author binze
 * 2020/6/8 17:03
 */
public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";
    private MatchRecorder recorder; //记录器
    private long period = ConfigUtil.getMatchPeriod();

    private final RecordState IDLE_STATE = new IdleState();
    private final RecordState RECORDING_STATE = new RecordingState();
    private final RecordState PAUSING_STATE = new PausingState();
    private final RecordState END_STATE = new EndState();
    private RecordState curState = IDLE_STATE;

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

    @Override
    public void onBackPressed() {
        curState.onBackPressed();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.action_settings).setVisible(false);
        return true;
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
                curState.onPressAction();
                return;
            case R.id.btn_plus_red:
                curState.onChangeScore(0, 1);
                return;
            case R.id.btn_plus_blue:
                curState.onChangeScore(1, 1);
                return;
            case R.id.btn_sub_red:
                curState.onChangeScore(0, -1);
                return;
            case R.id.btn_sub_blue:
                curState.onChangeScore(1, -1);
                return;
            default:
                break;
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

    private void superOnBackPressed() {
        runOnUiThread(super::onBackPressed);
    }

    private void changeState(RecordState state) {
        curState = state;
        curState.onStart();
    }

    ///////////////////////////////////////////////////////////////////////////
    // 状态
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 等待状态
     *
     * @author tong.xw 2021/01/06 12:18
     */
    private class IdleState implements RecordState {

        @Override
        public void onStart() {
            // do nothing...
        }

        @Override
        public void configUi() {
            // do nothing...
        }

        @Override
        public void onPressAction() {
            changeState(RECORDING_STATE);
        }

        @Override
        public void onChangeScore(int target, int score) {
            NoticeUtil.get().showToast("比赛尚未开始");
        }

        @Override
        public void onBackPressed() {
            superOnBackPressed();
        }
    }

    /**
     * 计时状态
     *
     * @author tong.xw 2021/01/06 12:21
     */
    private class RecordingState implements RecordState {

        @Override
        public void onStart() {
            configUi();
            recorder.start();
        }

        @Override
        public void configUi() {
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
                    public void onSubscribe(@NotNull Disposable d) {
                        dContainer.add(d);
                    }

                    @Override
                    public void onNext(@NotNull Long aLong) {
                        timerField.setText(getTimeByLong(aLong));
                    }

                    @Override
                    public void onError(@NotNull Throwable e) {
                        Log.e(TAG, "onError: 计时器出错", e);
                    }

                    @Override
                    public void onComplete() {
                        changeState(END_STATE);
                    }
                });
                timerField.setEnabled(false);
            }
            fabIcon.setImageResource(R.drawable.ic_pause);
        }

        @Override
        public void onPressAction() {
            changeState(PAUSING_STATE);
        }

        @Override
        public void onChangeScore(int target, int score) {
            NoticeUtil.get().showToast("为防误触，计时状态下禁止修改分数");
        }

        @Override
        public void onBackPressed() {
            NoticeUtil.get().showToast("为防误触，计时状态下屏蔽返回键");
        }
    }

    /**
     * 暂停状态
     *
     * @author tong.xw 2021/01/06 12:22
     */
    private class PausingState implements RecordState {

        @Override
        public void onStart() {
            recorder.pause();
            configUi();
        }

        @Override
        public void configUi() {
            fabIcon.setImageResource(R.drawable.ic_play);
        }

        @Override
        public void onPressAction() {
            changeState(RECORDING_STATE);
        }

        @Override
        public void onChangeScore(int target, int score) {
            changeScore(target, score);
        }

        @Override
        public void onBackPressed() {
            if (!checkCallAgain(2000)) {
                NoticeUtil.get().showToast("再次点击返回键结束比赛");
            } else {
                changeState(END_STATE);
            }
        }
    }

    /**
     * 结束状态
     *
     * @author tong.xw 2021/01/06 12:30
     */
    private class EndState implements RecordState {

        @Override
        public void onStart() {
            recorder.stop();
            configUi();
        }

        @Override
        public void configUi() {
            fabIcon.setImageResource(R.drawable.ic_complete);
            fabIcon.setEnabled(false);
            NoticeUtil.get().showToast("比赛结束，点击返回键保存并返回列表");
        }

        @Override
        public void onPressAction() {
            // do nothing...
        }

        @Override
        public void onChangeScore(int target, int score) {
            changeScore(target, score);
        }

        @Override
        public void onBackPressed() {
            ThreadUtil.get().execute(() -> {
                recorder.localizeRecord();
                superOnBackPressed();
            });
        }
    }
}
