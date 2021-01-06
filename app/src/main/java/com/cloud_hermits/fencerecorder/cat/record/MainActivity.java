package com.cloud_hermits.fencerecorder.cat.record;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

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
    private final long period = ConfigUtil.getMatchPeriod();

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

        timerField.setText(getTimeByLong(period));
    }

    @Override
    public void onBackPressed() {
        curState.onBackPressed();
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

    private void superOnBackPressed() {
        super.onBackPressed();
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
            changeScore(target, score);
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
                fabIcon.setImageResource(R.drawable.ic_pause);
            }
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
            configUi();
            recorder.pause();
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
