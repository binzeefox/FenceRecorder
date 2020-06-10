package com.cloud_hermits.fencerecorder.cat;

import android.os.Bundle;

import com.cloud_hermits.fencerecorder.R;
import com.cloud_hermits.fencerecorder.base.BaseActivity;

import java.util.Objects;

import androidx.appcompat.widget.Toolbar;

/**
 * 关于页
 *
 * @author binze
 * 2020/6/9 9:52
 */
public class AboutActivity extends BaseActivity {
    private static final String TAG = "AboutActivity";

    @Override
    protected int onSetLayoutResource() {
        return R.layout.activity_about;
    }

    @Override
    protected void create(Bundle savedInstanceState) {
        super.create(savedInstanceState);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("关于该软件");
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        getViewHelper().setTextById(R.id.tv_about, getAboutText());
    }

    private CharSequence getAboutText() {

        return getString(R.string.app_name_offline) +
                " 是由 杭州云栖剑社 成员狐彻开源的兵击活动计分工具。" +
                "该软件包含计分、计时、暂停、到时提醒和保存比赛记录的功能。" +
                "界面设计方便单手操控，适合作为小型比赛和对抗的计分记录工具。" +
                "\n\n" +
                "该软件尚在开发中，当前版本并非最终版本，可能会有记录丢失的情况发生。若出现该状况或其它异常状况，请联系 云栖剑社 进行反馈" +
                "\n\n" +
                "对剑术感兴趣，或对剑社感兴趣者，杭州云栖剑社欢迎各地友好的打剑仔前来交流" +
                "\n\n";
    }
}
