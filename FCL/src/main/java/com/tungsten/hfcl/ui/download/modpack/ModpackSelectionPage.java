package com.tungsten.hfcl.ui.download.modpack;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDialog;

import com.tungsten.hfcl.R;
import com.tungsten.hfcl.activity.MainActivity;
import com.tungsten.hfcl.setting.Profile;
import com.tungsten.hfcl.ui.PageManager;
import com.tungsten.hfcl.ui.TaskDialog;
import com.tungsten.hfcl.ui.download.DownloadPageManager;
import com.tungsten.hfcl.ui.manage.ManagePageManager;
import com.tungsten.hfcl.util.AndroidUtils;
import com.tungsten.hfcl.util.TaskCancellationAction;
import com.tungsten.hfclauncher.utils.FCLPath;
import com.tungsten.hfclcore.mod.server.ServerModpackManifest;
import com.tungsten.hfclcore.task.FileDownloadTask;
import com.tungsten.hfclcore.task.GetTask;
import com.tungsten.hfclcore.task.Schedulers;
import com.tungsten.hfclcore.task.Task;
import com.tungsten.hfclcore.task.TaskExecutor;
import com.tungsten.hfclcore.util.gson.JsonUtils;
import com.tungsten.hfcllibrary.component.ui.FCLTempPage;
import com.tungsten.hfcllibrary.component.view.FCLLinearLayout;
import com.tungsten.hfcllibrary.component.view.FCLUILayout;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class ModpackSelectionPage extends FCLTempPage implements View.OnClickListener {

    private final Profile profile;
    private final String updateVersion;

    private FCLLinearLayout local;
    private FCLLinearLayout remote;

    public ModpackSelectionPage(Context context, int id, FCLUILayout parent, int resId, Profile profile, String updateVersion) {
        super(context, id, parent, resId);
        this.profile = profile;
        this.updateVersion = updateVersion;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        local = findViewById(R.id.local);
        remote = findViewById(R.id.remote);

        local.setOnClickListener(this);
        remote.setOnClickListener(this);
    }

    private void onChooseLocalFile() {
        ArrayList<String> suffix = new ArrayList<>();
        suffix.add(".zip");
        suffix.add(".mrpack");
        suffix.add(".7z");
        suffix.add(".rar");
        MainActivity.getInstance().fileLauncher.launchSingleSelection(null, suffix, files -> {
            if (files == null) return;
            String path = files.get(0);
            Uri uri = Uri.parse(path);
            if (AndroidUtils.isDocUri(uri)) {
                path = AndroidUtils.copyFileToDir(getActivity(), uri, new File(FCLPath.CACHE_DIR));
            }
            if (path == null)
                return;
            File selectedFile = new File(path);
            Schedulers.androidUIThread().execute(() -> {
                LocalModpackPage page = new LocalModpackPage(getContext(), PageManager.PAGE_ID_TEMP, getParent(), R.layout.page_modpack, profile, updateVersion, selectedFile);
                if (updateVersion == null) {
                    DownloadPageManager.getInstance().dismissCurrentTempPage();
                    DownloadPageManager.getInstance().showTempPage(page);
                } else {
                    ManagePageManager.getInstance().dismissCurrentTempPage();
                    ManagePageManager.getInstance().showTempPage(page);
                }
            });
        });
    }

    private void onChooseRemoteFile() {
        ModpackUrlDialog dialog = new ModpackUrlDialog(getContext(), urlString -> {
            try {
                URL url = new URL(urlString);
                if (urlString.endsWith("server-manifest.json")) {
                    // if urlString ends with .json, we assume that the url is server-manifest.json
                    TaskDialog taskDialog = new TaskDialog(getContext(), new TaskCancellationAction(AppCompatDialog::dismiss));
                    taskDialog.setTitle(getContext().getString(R.string.message_downloading));
                    TaskExecutor executor = new GetTask(url).whenComplete(Schedulers.androidUIThread(), (result, e) -> {
                        ServerModpackManifest manifest = JsonUtils.fromMaybeMalformedJson(result, ServerModpackManifest.class);
                        if (manifest == null) {
                            Toast.makeText(getContext(), getContext().getString(R.string.modpack_type_server_malformed), Toast.LENGTH_SHORT).show();
                        } else if (e == null) {
                            RemoteModpackPage page = new RemoteModpackPage(getContext(), PageManager.PAGE_ID_TEMP, getParent(), R.layout.page_modpack, profile, updateVersion, manifest);
                            if (updateVersion == null) {
                                DownloadPageManager.getInstance().dismissCurrentTempPage();
                                DownloadPageManager.getInstance().showTempPage(page);
                            } else {
                                ManagePageManager.getInstance().dismissCurrentTempPage();
                                ManagePageManager.getInstance().showTempPage(page);
                            }
                        } else {
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }).executor();
                    taskDialog.setExecutor(executor);
                    taskDialog.show();
                    executor.start();
                } else {
                    // otherwise we still consider the file as modpack zip file
                    // since casually the url may not ends with ".zip"
                    Path modpack = Files.createTempFile("modpack", ".zip");

                    TaskDialog taskDialog = new TaskDialog(getContext(), new TaskCancellationAction(AppCompatDialog::dismiss));
                    taskDialog.setTitle(getContext().getString(R.string.message_downloading));
                    TaskExecutor executor = new FileDownloadTask(url, modpack.toFile(), null)
                            .whenComplete(Schedulers.androidUIThread(), e -> {
                                if (e == null) {
                                    LocalModpackPage page = new LocalModpackPage(getContext(), PageManager.PAGE_ID_TEMP, getParent(), R.layout.page_modpack, profile, updateVersion, modpack.toFile());
                                    if (updateVersion == null) {
                                        DownloadPageManager.getInstance().dismissCurrentTempPage();
                                        DownloadPageManager.getInstance().showTempPage(page);
                                    } else {
                                        ManagePageManager.getInstance().dismissCurrentTempPage();
                                        ManagePageManager.getInstance().showTempPage(page);
                                    }
                                } else {
                                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }).executor();
                    taskDialog.setExecutor(executor);
                    taskDialog.show();
                    executor.start();
                }
            } catch (IOException e) {
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    @Override
    public Task<?> refresh(Object... param) {
        return null;
    }

    @Override
    public void onRestart() {

    }

    @Override
    public void onClick(View v) {
        if (v == local) {
            onChooseLocalFile();
        }
        if (v == remote) {
            onChooseRemoteFile();
        }
    }
}
