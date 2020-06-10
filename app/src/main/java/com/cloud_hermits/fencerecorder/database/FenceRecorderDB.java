package com.cloud_hermits.fencerecorder.database;

import com.cloud_hermits.fencerecorder.database.tables.Match;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

/**
 * 记录器数据库
 * @author binze
 * 2020/6/8 14:46
 */
@Database(entities = {Match.class}, version = 1, exportSchema = false)
public abstract class FenceRecorderDB extends RoomDatabase {
    public abstract Match.Api selectMatch();
}
