package com.tungsten.hfcl.ui.download.version;

import static com.tungsten.hfclcore.util.Logging.LOG;

import android.content.Context;
import android.view.View;
import android.widget.CompoundButton;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tungsten.hfcl.R;
import com.tungsten.hfcl.setting.DownloadProviders;
import com.tungsten.hfcl.ui.PageManager;
import com.tungsten.hfcl.ui.download.DownloadPageManager;
import com.tungsten.hfclcore.download.RemoteVersion;
import com.tungsten.hfclcore.download.VersionList;
import com.tungsten.hfclcore.task.Schedulers;
import com.tungsten.hfclcore.task.Task;
import com.tungsten.hfclcore.util.versioning.GameVersionNumber;
import com.tungsten.hfcllibrary.component.ui.FCLCommonPage;
import com.tungsten.hfcllibrary.component.view.FCLCheckBox;
import com.tungsten.hfcllibrary.component.view.FCLEditText;
import com.tungsten.hfcllibrary.component.view.FCLImageButton;
import com.tungsten.hfcllibrary.component.view.FCLProgressBar;
import com.tungsten.hfcllibrary.component.view.FCLUILayout;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class VersionInstallPage extends FCLCommonPage implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private FCLCheckBox checkRelease;
    private FCLCheckBox checkSnapShot;
    private FCLCheckBox checkOld;
    private FCLCheckBox checkAprilFools;
    private FCLImageButton refresh;
    private FCLImageButton failedRefresh;
    private FCLProgressBar progressBar;
    private RecyclerView recyclerView;
    private FCLEditText search;

    private RemoteVersionListAdapter.OnRemoteVersionSelectListener listener;

    public VersionInstallPage(Context context, int id, FCLUILayout parent, int resId) {
        super(context, id, parent, resId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        checkRelease = findViewById(R.id.release);
        checkSnapShot = findViewById(R.id.snapshot);
        checkOld = findViewById(R.id.old);
        checkAprilFools = findViewById(R.id.april_fools);
        refresh = findViewById(R.id.refresh);
        failedRefresh = findViewById(R.id.failed_refresh);
        progressBar = findViewById(R.id.progress);
        recyclerView = findViewById(R.id.list);
        search = findViewById(R.id.search);

        checkRelease.setChecked(true);

        checkRelease.setOnCheckedChangeListener(this);
        checkSnapShot.setOnCheckedChangeListener(this);
        checkOld.setOnCheckedChangeListener(this);
        checkAprilFools.setOnCheckedChangeListener(this);
        refresh.setOnClickListener(this);
        failedRefresh.setOnClickListener(this);

        listener = remoteVersion -> {
            VersionInstallInfoPage page = new VersionInstallInfoPage(getContext(), PageManager.PAGE_ID_TEMP, getParent(), R.layout.page_installer, remoteVersion.getGameVersion());
            DownloadPageManager.getInstance().showTempPage(page);
        };

        search.stringProperty().addListener(observable -> refreshDisplayVersions());

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        refreshList();
    }

    private List<RemoteVersion> loadVersions() {
        return DownloadProviders.getDownloadProvider().getVersionListById("game").getVersions("").stream()
                .filter(it -> switch (it.getVersionType()) {
                    case RELEASE -> checkRelease.isChecked();
                    case PENDING, UNOBFUSCATED, SNAPSHOT -> {
                        if (checkSnapShot.isChecked()) yield true;
                        else if (checkAprilFools.isChecked())
                            yield GameVersionNumber.asGameVersion(it.getGameVersion()).isAprilFools();
                        yield false;
                    }
                    case OLD -> {
                        if (checkOld.isChecked()) yield true;
                        else if (checkAprilFools.isChecked())
                            yield GameVersionNumber.asGameVersion(it.getGameVersion()).isAprilFools();
                        yield false;
                    }
                    default -> true;
                })
                .filter(it -> it.getGameVersion().contains(search.getStringValue()))
                .sorted().collect(Collectors.toList());
    }

    public void refreshDisplayVersions() {
        List<RemoteVersion> items = loadVersions();
        RemoteVersionListAdapter adapter = new RemoteVersionListAdapter(getContext(), (ArrayList<RemoteVersion>) items, listener);
        recyclerView.setAdapter(adapter);
    }

    public void refreshList() {
        recyclerView.setVisibility(View.GONE);
        failedRefresh.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        refresh.setEnabled(false);
        search.setText("");
        VersionList<?> currentVersionList = DownloadProviders.getDownloadProvider().getVersionListById("game");
        currentVersionList.refreshAsync("").whenComplete((result, exception) -> {
            if (exception == null) {
                List<RemoteVersion> items = loadVersions();

                Schedulers.androidUIThread().execute(() -> {
                    if (items.isEmpty()) {
                        checkRelease.setChecked(true);
                        checkSnapShot.setChecked(true);
                        checkOld.setChecked(true);
                    } else {
                        RemoteVersionListAdapter adapter = new RemoteVersionListAdapter(getContext(), (ArrayList<RemoteVersion>) items, listener);
                        recyclerView.setAdapter(adapter);
                    }
                    recyclerView.setVisibility(View.VISIBLE);
                    failedRefresh.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    refresh.setEnabled(true);
                });
            } else {
                LOG.log(Level.WARNING, "Failed to fetch versions list", exception);
                Schedulers.androidUIThread().execute(() -> {
                    recyclerView.setVisibility(View.GONE);
                    failedRefresh.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    refresh.setEnabled(true);
                });
            }

            System.gc();
        });
    }

    @Override
    public Task<?> refresh(Object... param) {
        return Task.runAsync(() -> {

        });
    }

    @Override
    public void onClick(View view) {
        if (view == refresh || view == failedRefresh) {
            refreshList();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (compoundButton == checkRelease || compoundButton == checkSnapShot || compoundButton == checkOld || compoundButton == checkAprilFools) {
            refreshDisplayVersions();
        }
    }
}
