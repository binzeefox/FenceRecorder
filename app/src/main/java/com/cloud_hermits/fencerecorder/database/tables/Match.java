package com.cloud_hermits.fencerecorder.database.tables;

import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Update;

/**
 * 单场对决数据模型
 * @author binze
 * 2020/6/8 14:42
 */
@Entity
public class Match {
    private static final String TAG = "MatchModel";

    @PrimaryKey
    private long timestamp = new Date().getTime();

    @ColumnInfo(name = "period")
    private long period;

    @ColumnInfo(name = "red_name")
    private String redName;

    @ColumnInfo(name = "blue_name")
    private String blueName;

    @ColumnInfo(name = "red_score")
    private int redScore;

    @ColumnInfo(name = "blue_score")
    private int blueScore;

    @ColumnInfo(name = "comment")
    private String comment;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getPeriod() {
        return period;
    }

    public void setPeriod(long period) {
        this.period = period;
    }

    public String getRedName() {
        return redName;
    }

    public void setRedName(String redName) {
        this.redName = redName;
    }

    public String getBlueName() {
        return blueName;
    }

    public void setBlueName(String blueName) {
        this.blueName = blueName;
    }

    public int getRedScore() {
        return redScore;
    }

    public void setRedScore(int redScore) {
        this.redScore = redScore;
    }

    public int getBlueScore() {
        return blueScore;
    }

    public void setBlueScore(int blueScore) {
        this.blueScore = blueScore;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Dao
    public interface Api{
        /**
         * 全部记录
         * @author binze 2020/6/8 15:03
         */
        @Query("SELECT * FROM `match`")
        List<Match> getAll();

        /**
         * 通过时间戳批量搜索
         * @author binze 2020/6/8 15:02
         */
        @Query("SELECT * FROM `match` WHERE timestamp IN (:timestamps)")
        List<Match> queryAllByTimestamp(long[] timestamps);

        /**
         * 通过时间段查询
         * @author binze 2020/6/8 15:07
         */
        @Query("SELECT * FROM `match` WHERE timestamp >= :start AND timestamp <= :end")
        List<Match> queryAllFromTimeRange(long start, long end);

        @Update
        void update(Match match);

        /**
         * 新增操作
         * @author binze 2020/6/8 15:10
         */
        @Insert
        void insertAll(Match... matches);

        /**
         * 删除操作
         * @author binze 2020/6/8 15:10
         */
        @Delete
        void delete(Match match);

        @Delete
        int deleteAll(Match... matches);

        @Delete
        int deleteAll(List<Match> matches);
    }
}
