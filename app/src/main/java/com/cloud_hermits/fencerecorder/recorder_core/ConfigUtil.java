package com.cloud_hermits.fencerecorder.recorder_core;

import android.annotation.SuppressLint;
import android.text.TextUtils;

import com.binzeefox.foxframe.tools.resource.SharedPreferenceUtil;

public class ConfigUtil {
    private static final String KEY_MATCH_PERIOD = "key_match_period";  //比赛时长

    @SuppressLint("StaticFieldLeak")    //Application Context
    private static SharedPreferenceUtil util = SharedPreferenceUtil.get();

    private static final long DEFAULT_PERIOD = 3 * 60 * 1000;   //默认单局时长毫秒

    /**
     * 获取比赛时长
     * @author binze 2019/12/16 10:41
     */
    public static long getMatchPeriod(){
        long period = DEFAULT_PERIOD;
        String periodStr = util.readConfig(KEY_MATCH_PERIOD);
        if (!TextUtils.isEmpty(periodStr))
            period = Long.parseLong(periodStr);
        return period;
    }

    /**
     * 设置单局时长
     * @author binze 2019/12/16 10:59
     */
    public static void setMatchPeriod(long period){
        util.writeConfig(KEY_MATCH_PERIOD, "" + period);
    }
}
