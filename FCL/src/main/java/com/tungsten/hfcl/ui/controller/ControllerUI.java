package com.tungsten.hfcl.ui.controller;

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

public class ControllerUI extends FCLMultiPageUI {

    private ControllerPageManager pageManager;

    private FCLUILayout container;
    private Runnable runnable;

    public ControllerUI(Context context, FCLUILayout parent, int id) {
        super(context, parent, id);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        container = findViewById(R.id.container);
        container.post(this::initPages);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onBackPressed() {
        if (pageManager != null && pageManager.canReturn()) {
            pageManager.dismissCurrentTempPage();
        } else if (pageManager != null && pageManager.getCurrentPage() instanceof ControllerRepoPage) {
            pageManager.switchPage(ControllerPageManager.PAGE_ID_CONTROLLER_MANAGER);
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
        pageManager = new ControllerPageManager(getContext(), container, ControllerPageManager.PAGE_ID_CONTROLLER_MANAGER);
        if (runnable != null) {
            runnable.run();
        }
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
        return null;
    }

    public ControllerPageManager getPageManager() {
        return pageManager;
    }

    @Override
    public void runAfterInit(Runnable runnable) {
        this.runnable = runnable;
        if (pageManager != null) {
            runnable.run();
        }
    }
}
