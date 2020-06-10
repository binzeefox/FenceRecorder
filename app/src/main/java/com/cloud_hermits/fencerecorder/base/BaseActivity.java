package com.cloud_hermits.fencerecorder.base;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import com.binzeefox.foxframe.core.base.FoxActivity;

import butterknife.ButterKnife;

public abstract class BaseActivity extends FoxActivity {
    public static final String GLOBAL_KEY_MATCHES = "Match_Data";

    @Override
    protected void create(Bundle savedInstanceState) {
        ButterKnife.bind(this);
        View decorView = getWindow().getDecorView();
        int option =
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(option);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
    }
}
