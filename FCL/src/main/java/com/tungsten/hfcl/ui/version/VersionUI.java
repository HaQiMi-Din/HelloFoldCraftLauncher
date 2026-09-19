package com.tungsten.hfcl.ui.version;

import static com.tungsten.hfclcore.util.Lang.tryCast;

import android.content.Context;

import com.tungsten.hfcl.R;
import com.tungsten.hfclcore.task.Task;
import com.tungsten.hfcllibrary.component.ui.FCLBasePage;
import com.tungsten.hfcllibrary.component.ui.FCLMultiPageUI;
import com.tungsten.hfcllibrary.component.view.FCLUILayout;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

public class VersionUI extends FCLMultiPageUI {

    private VersionPageManager pageManager;

    private FCLUILayout container;

    public VersionUI(Context context, FCLUILayout parent, int id) {
        super(context, parent, id);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        container = findViewById(R.id.container);
        container.post(this::initPages);
    }

    @Override
    public void onBackPressed() {
        if (pageManager != null && pageManager.canReturn()) {
            pageManager.dismissCurrentTempPage();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (pageManager != null) {
            pageManager.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (pageManager != null) {
            pageManager.onResume();
        }
    }

    @Override
    public void initPages() {
        pageManager = new VersionPageManager(getContext(), container, VersionPageManager.PAGE_ID_VERSION_LIST);
    }

    @Override
    public ArrayList<FCLBasePage> getAllPages() {
        return pageManager == null ? null : (ArrayList<FCLBasePage>) pageManager.getAllPages().stream().map(it -> tryCast(it, FCLBasePage.class)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    @Override
    public FCLBasePage getPage(int id) {
        return pageManager == null ? null : pageManager.getPageById(id);
    }

    @Override
    public Task<?> refresh(Object... param) {
        return Task.runAsync(() -> {

        });
    }
}
