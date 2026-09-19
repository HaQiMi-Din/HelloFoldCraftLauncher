package com.tungsten.hfcl.ui.setting;

import android.content.Context;
import android.view.View;

import com.tungsten.hfcl.R;
import com.tungsten.hfcl.util.AndroidUtils;
import com.tungsten.hfclcore.task.Task;
import com.tungsten.hfcllibrary.component.ui.FCLCommonPage;
import com.tungsten.hfcllibrary.component.view.FCLLinearLayout;
import com.tungsten.hfcllibrary.component.view.FCLUILayout;

public class AboutPage extends FCLCommonPage implements View.OnClickListener {

    private FCLLinearLayout source;

    public AboutPage(Context context, int id, FCLUILayout parent, int resId) {
        super(context, id, parent, resId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        source = findViewById(R.id.source);
        source.setOnClickListener(this);
    }

    @Override
    public Task<?> refresh(Object... param) {
        return null;
    }

    @Override
    public void onClick(View v) {
        if (v == source) {
            AndroidUtils.openLink(getContext(), "https://github.com/HaQiMi-Din/HelloFoldCraftLauncher");
        }
    }
}
