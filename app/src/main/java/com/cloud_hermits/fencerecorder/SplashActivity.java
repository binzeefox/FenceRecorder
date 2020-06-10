package com.cloud_hermits.fencerecorder;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.binzeefox.foxframe.core.FoxCore;
import com.binzeefox.foxframe.core.base.FoxActivity;
import com.binzeefox.foxframe.core.base.callbacks.PermissionCallback;
import com.binzeefox.foxframe.tools.dev.ThreadUtil;
import com.binzeefox.foxframe.tools.phone.NoticeUtil;
import com.cloud_hermits.fencerecorder.base.BaseActivity;
import com.cloud_hermits.fencerecorder.base.BaseApplication;
import com.cloud_hermits.fencerecorder.cat.ListActivity;
import com.cloud_hermits.fencerecorder.database.tables.Match;
import com.tencent.bugly.beta.Beta;

import java.util.Arrays;
import java.util.List;

public class SplashActivity extends BaseActivity {
    private static final String TAG = "SplashActivity";

    @Override
    protected int onSetLayoutResource() {
        return R.layout.activity_splash;
    }

    @Override
    protected void create(Bundle savedInstanceState) {
        super.create(savedInstanceState);

        List<String> permissionList = Arrays.asList(
//                Manifest.permission.BLUETOOTH,
//                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        );
        requestPermission(permissionList, failedList -> {
            if (!failedList.isEmpty())
                NoticeUtil.get().showToast("有权限尚未通过，可能会影响该APP正常使用");
            startApp();
        });
    }

    /**
     * 倒计时并进入主页面
     * @author binze 2020/6/8 15:27
     */
    private void startApp() {
        ThreadUtil.get().execute(() -> {
            List<Match> matchList = BaseApplication.getDatabase().selectMatch().getAll();
            Log.d(TAG, "startApp: match count: " + matchList.size());
            FoxCore.get().putGlobalData(GLOBAL_KEY_MATCHES, matchList);
            runOnUiThread(() -> {
                Handler handler = new Handler(msg -> {
                    navigate(ListActivity.class).commit();
                    finish();
                    return true;
                });
                handler.sendEmptyMessageDelayed(0, 1500);
            });
        });
    }
}