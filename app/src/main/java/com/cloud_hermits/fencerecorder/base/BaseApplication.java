package com.cloud_hermits.fencerecorder.base;

import com.binzeefox.foxframe.core.base.FoxApplication;
import com.cloud_hermits.fencerecorder.database.FenceRecorderDB;
import com.cloud_hermits.fencerecorder.database.tables.Match;
import com.tencent.bugly.Bugly;

import java.util.List;

import androidx.room.Room;

import static com.cloud_hermits.fencerecorder.recorder_core.MatchRecorder.DB_NAME;

public class BaseApplication extends FoxApplication {
    private static final String TAG = "BaseApplication";
    public static final boolean DEBUG_MODE = false;
    private static final String BUGLY_KEY = "2650df2f-9e8d-4f5b-88b7-737245718434";
    private static final String BUGLY_ID = "ee40e71ee1";
    private static FenceRecorderDB mDatabase;

    @Override
    public void onCreate() {
        super.onCreate();
        Bugly.init(this, BUGLY_ID, DEBUG_MODE);

        mDatabase = Room.databaseBuilder
                (this, FenceRecorderDB.class, DB_NAME)
                .build();
    }

    /**
     * 获取数据库实例
     * @author binze 2020/6/8 14:51
     */
    public static FenceRecorderDB getDatabase(){
        return mDatabase;
    }

    /**
     * 清除match表
     * @author binze 2020/6/9 11:28
     */
    public static boolean clearMatchTable(){
        List<Match> list = getDatabase().selectMatch().getAll();
        if (list.isEmpty()) return true;
        return getDatabase().selectMatch().deleteAll(list) != 0;
    }
}
