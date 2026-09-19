package com.tungsten.hfcl.ui.download.common;

import android.content.Context;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatDialog;

import com.tungsten.hfcl.R;
import com.tungsten.hfcl.activity.MainActivity;
import com.tungsten.hfcl.setting.Profile;
import com.tungsten.hfcl.ui.PageManager;
import com.tungsten.hfcl.ui.TaskDialog;
import com.tungsten.hfcl.ui.download.DownloadPageManager;
import com.tungsten.hfcl.util.TaskCancellationAction;
import com.tungsten.hfclcore.mod.RemoteMod;
import com.tungsten.hfclcore.task.FileDownloadTask;
import com.tungsten.hfclcore.task.Schedulers;
import com.tungsten.hfclcore.task.Task;
import com.tungsten.hfclcore.task.TaskExecutor;
import com.tungsten.hfclcore.util.io.NetworkUtils;
import com.tungsten.hfcllibrary.component.ui.FCLTempPage;
import com.tungsten.hfcllibrary.component.view.FCLUILayout;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

public class RemoteModVersionPage extends FCLTempPage {

    private final List<RemoteMod.Version> list;
    private final Profile.ProfileVersion version;
    private final RemoteModVersionPage.DownloadCallback callback;
    private final DownloadPage downloadPage;

    private ListView listView;

    public RemoteModVersionPage(Context context, int id, FCLUILayout parent, int resId, List<RemoteMod.Version> list, Profile.ProfileVersion version, @Nullable RemoteModVersionPage.DownloadCallback callback, DownloadPage downloadPage) {
        super(context, id, parent, resId);
        this.list = list;
        this.version = version;
        this.callback = callback;
        this.downloadPage = downloadPage;
    }

    @Override
    public void onStart() {
        super.onStart();
        listView = findViewById(R.id.list);
        ModVersionAdapter adapter = new ModVersionAdapter(getContext(), list, version -> {
            if (downloadPage.getId() == DownloadPageManager.PAGE_ID_DOWNLOAD_MOD) {
                RemoteModDownloadPage page = new RemoteModDownloadPage(getContext(), PageManager.PAGE_ID_TEMP, getParent(), R.layout.page_download_addon, this.version, version, callback, this, downloadPage);
                DownloadPageManager.getInstance().showTempPage(page);
            } else {
                download(version);
            }
        });
        listView.setAdapter(adapter);
    }

    public void download(RemoteMod.Version file) {
        if (this.callback == null) {
            saveAs(file);
        } else {
            this.callback.download(version.getProfile(), version.getVersion(), file);
        }
    }

    public void saveAs(RemoteMod.Version file) {
        MainActivity.getInstance().fileLauncher.launchSingleSelection(null, null, true, files -> {
            if (files == null) return;
            String folder = files.get(0);
            if (folder == null)
                return;
            TaskDialog dialog = new TaskDialog(getContext(), new TaskCancellationAction(AppCompatDialog::dismiss));
            dialog.setTitle(getContext().getString(R.string.message_downloading));
            Schedulers.androidUIThread().execute(() -> {
                TaskExecutor executor = Task.composeAsync(() -> {
                    FileDownloadTask task = new FileDownloadTask(NetworkUtils.toURL(file.getFile().getUrl()), new File(folder, file.getFile().getFilename()), file.getFile().getIntegrityCheck());
                    task.setName(file.getName());
                    return task;
                }).executor();
                dialog.setExecutor(executor);
                dialog.show();
                executor.start();
            });
        });
    }

    @Override
    public Task<?> refresh(Object... param) {
        return null;
    }

    @Override
    public void onRestart() {

    }

    public interface DownloadCallback {
        void download(Profile profile, @Nullable String version, RemoteMod.Version file);
    }
}
