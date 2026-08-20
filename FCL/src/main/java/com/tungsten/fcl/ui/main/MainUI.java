package com.tungsten.fcl.ui.main;

import android.content.Context;
import android.view.View;

import com.tungsten.fcllibrary.component.ui.FCLCommonUI;
import com.tungsten.fcllibrary.component.view.FCLUILayout;
import com.tungsten.fclcore.task.Task;

public class MainUI extends FCLCommonUI implements View.OnClickListener {

    public MainUI(Context context, FCLUILayout parent, int id) {
        super(context, parent, id);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public Task<?> refresh(Object... param) {
        return Task.runAsync(() -> {

        });
    }

    public void refreshSkin(com.tungsten.fclcore.auth.Account account) {
    }

    @Override
    public void onClick(View view) {
    }
}
