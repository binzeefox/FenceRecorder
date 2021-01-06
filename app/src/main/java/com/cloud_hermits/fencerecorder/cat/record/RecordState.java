package com.cloud_hermits.fencerecorder.cat.record;

/**
 * 记录状态
 *
 * @author tong.xw
 * 2021/01/06 12:11
 */
public interface RecordState {

    /**
     * 进入该状态
     *
     * @author tong.xw 2021/01/06 12:24
     */
    void onStart();

    /**
     * 修改UI
     *
     * @author tong.xw 2021/01/06 12:13
     */
    void configUi();

    /**
     * 点击动作键
     *
     * @author tong.xw 2021/01/06 12:35
     */
    void onPressAction();

    /**
     * 改变分数
     *
     * @param target 目标 0为红，1为蓝
     * @param score  分数
     * @author tong.xw 2021/01/06 12:13
     */
    void onChangeScore(int target, int score);

    void onBackPressed();
}
