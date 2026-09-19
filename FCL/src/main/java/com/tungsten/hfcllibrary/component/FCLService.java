package com.tungsten.hfcllibrary.component;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tungsten.hfclauncher.utils.FCLPath;
import com.tungsten.hfcllibrary.component.theme.ThemeEngine;
import com.tungsten.hfcllibrary.util.LocaleUtils;

public class FCLService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        FCLPath.loadPaths(this);
        ThemeEngine.getInstance().setupThemeEngine(this);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtils.setLanguage(base));
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleUtils.setLanguage(this);
    }
}
