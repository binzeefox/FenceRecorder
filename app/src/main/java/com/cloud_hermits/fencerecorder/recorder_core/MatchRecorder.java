package com.cloud_hermits.fencerecorder.recorder_core;

import android.annotation.SuppressLint;
import android.content.Context;

import com.binzeefox.foxframe.core.FoxCore;
import com.binzeefox.foxframe.tools.RxUtil;
import com.binzeefox.foxframe.tools.dev.ThreadUtil;
import com.binzeefox.foxframe.tools.phone.NoticeUtil;
import com.cloud_hermits.fencerecorder.base.BaseApplication;
import com.cloud_hermits.fencerecorder.database.tables.Match;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;

import static com.cloud_hermits.fencerecorder.base.BaseActivity.GLOBAL_KEY_MATCHES;

/**
 * 计分器类
 *
 * @author binze
 * 2020/6/8 14:19
 */
public class MatchRecorder implements Controller {
    private static final String TAG = "MatchRecorder";
    public static final String DB_NAME = "FenceRecorderDB";
    private final Context mCtx = FoxCore.getApplication();
    private final long maxTime; //总时常
    @SuppressLint("StaticFieldLeak")    //Application Context
//    private static MatchRecorder sInstance;

    private long curTime;   //当前时间
    private String blueSide, redSide;   //双方选手名称
    private int[] score = new int[]{0, 0};   //红，蓝分数
    private boolean running = true;    //工作状态
    private boolean pausing = true;
    private final Observable<Long> timerObservable;


    /**
     * 单例
     * @author binze 2020/6/8 14:37
     */
//    public static MatchRecorder get(long period, String blueSide, String redSide){
//        if (sInstance == null){
//            sInstance = new MatchRecorder(period, blueSide, redSide);
//        }
//        return sInstance;
//    }

    /**
     * 返回计时器被观察者
     * @author binze 2019/12/17 10:34
     */
    public Observable<Long> observable(){
        return timerObservable;
    }

    /**
     * 初始化
     *
     * @author binze 2020/6/8 14:19
     */
    public MatchRecorder(long period, String blueSide, String redSide) {
        maxTime = period;
        this.blueSide = blueSide;
        this.redSide = redSide;
        timerObservable = Observable.create((ObservableOnSubscribe<Long>) emitter -> {
            curTime = maxTime;
            while (curTime > 0) {
                if (!running || pausing) continue;
                emitter.onNext(curTime);
                curTime -= 1000;
                if (curTime < 0) curTime = 0;
                Thread.sleep(1000);
            }
            emitter.onNext(0L);

            // 震动
            NoticeUtil.get().vibrate().vibrate(3000);

            stop();
            emitter.onComplete();
        }).compose(RxUtil.setThreadComputation());
    }

    /**
     * 是否在工作
     *
     * @author binze 2020/6/8 14:23
     */
    public boolean isRunning() {
        return running && !pausing;
    }

    /**
     * 当前比分
     *
     * @return 0红方，1蓝方
     * @author binze 2020/6/8 14:23
     */
    public int[] getScore() {
        return score;
    }

    /**
     * 红方得分
     *
     * @author binze 2020/6/8 14:27
     */
    public void scoreRed(int score) {
        this.score[0] += score;
    }

    /**
     * 蓝方得分
     *
     * @author binze 2020/6/8 14:27
     */
    public void scoreBlue(int score) {
        this.score[1] += score;
    }

    @Override
    public void start() {
        if (running)
            pausing = false;
    }

    @Override
    public void pause() {
        if (running)
            pausing = true;
    }

    @Override
    public void stop() {
        pausing = true;
        running = false;

        // 记录比赛到本地
//        Match match = new Match();
//        match.setBlueName(blueSide);
//        match.setRedName(redSide);
//        match.setPeriod(maxTime - curTime);
//        match.setRedScore(score[0]);
//        match.setBlueScore(score[1]);
//        ThreadUtil.get().execute(() -> {
//            BaseApplication.getDatabase().selectMatch().insertAll(match);
//            List<Match> list = FoxCore.get().getGlobalData(GLOBAL_KEY_MATCHES, new ArrayList<>());
//            list.add(match);
//            FoxCore.get().putGlobalData(GLOBAL_KEY_MATCHES, list);
//        });
    }

    /**
     * 同步方法，将Record保存到本地
     *
     * @author tong.xw 2021/01/06 12:48
     */
    public void localizeRecord() {
        Match match = new Match();
        match.setBlueName(blueSide);
        match.setRedName(redSide);
        match.setPeriod(maxTime - curTime);
        match.setRedScore(score[0]);
        match.setBlueScore(score[1]);
        BaseApplication.getDatabase().selectMatch().insertAll(match);
        List<Match> list = FoxCore.get().getGlobalData(GLOBAL_KEY_MATCHES, new ArrayList<>());
        list.add(match);
        FoxCore.get().putGlobalData(GLOBAL_KEY_MATCHES, list);
    }
}
