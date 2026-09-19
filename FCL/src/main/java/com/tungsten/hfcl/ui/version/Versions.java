package com.tungsten.hfcl.ui.version;

import android.content.Context;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDialog;

import com.mio.util.ParseUtil;
import com.tungsten.hfcl.R;
import com.tungsten.hfcl.activity.MainActivity;
import com.tungsten.hfcl.game.LauncherHelper;
import com.tungsten.hfcl.setting.Accounts;
import com.tungsten.hfcl.setting.Profile;
import com.tungsten.hfcl.setting.Profiles;
import com.tungsten.hfcl.ui.PageManager;
import com.tungsten.hfcllibrary.ui.ProgressDialog;
import com.tungsten.hfcl.ui.TaskDialog;
import com.tungsten.hfcl.ui.account.CreateAccountDialog;
import com.tungsten.hfcl.ui.download.DownloadPageManager;
import com.tungsten.hfcl.ui.download.modpack.LocalModpackPage;
import com.tungsten.hfcl.ui.download.modpack.ModpackSelectionPage;
import com.tungsten.hfcl.ui.manage.ManagePageManager;
import com.tungsten.hfcl.ui.manage.ModpackTypeSelectionPage;
import com.tungsten.hfcl.util.AndroidUtils;
import com.tungsten.hfcl.util.TaskCancellationAction;
import com.tungsten.hfclcore.auth.Account;
import com.tungsten.hfclcore.auth.AccountFactory;
import com.tungsten.hfclcore.download.game.GameAssetDownloadTask;
import com.tungsten.hfclcore.mod.RemoteMod;
import com.tungsten.hfclcore.task.FileDownloadTask;
import com.tungsten.hfclcore.task.Schedulers;
import com.tungsten.hfclcore.task.Task;
import com.tungsten.hfclcore.task.TaskExecutor;
import com.tungsten.hfclcore.util.Logging;
import com.tungsten.hfclcore.util.StringUtils;
import com.tungsten.hfclcore.util.platform.OperatingSystem;
import com.tungsten.hfcllibrary.component.dialog.FCLAlertDialog;
import com.tungsten.hfcllibrary.component.view.FCLUILayout;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;

public class Versions {

    public static void importModpack(Context context, FCLUILayout parent) {
        Profile profile = Profiles.getSelectedProfile();
        if (profile.getRepository().isLoaded()) {
            ModpackSelectionPage page = new ModpackSelectionPage(context, PageManager.PAGE_ID_TEMP, parent, R.layout.page_modpack_selection, profile, null);
            DownloadPageManager.getInstance().showTempPage(page);
        }
    }

    public static void downloadModpackImpl(Context context, FCLUILayout parent, Profile profile, RemoteMod.Version file) {
        Path modpack;
        URL downloadURL;
        try {
            modpack = Files.createTempFile("modpack", ".zip");
            downloadURL = new URL(file.getFile().getUrl());
        } catch (IOException e) {
            FCLAlertDialog.Builder builder = new FCLAlertDialog.Builder(context);
            builder.setAlertLevel(FCLAlertDialog.AlertLevel.ALERT);
            builder.setCancelable(false);
            builder.setTitle(context.getString(R.string.download_failed));
            builder.setMessage(AndroidUtils.getLocalizedText(context, "install_failed_downloading_detail", file.getFile().getUrl()) + "\n" + StringUtils.getStackTrace(e));
            builder.setNegativeButton(context.getString(com.tungsten.hfcl.R.string.dialog_positive), null);
            builder.create().show();
            return;
        }

        TaskDialog taskDialog = new TaskDialog(context, new TaskCancellationAction(AppCompatDialog::dismiss));
        taskDialog.setTitle(context.getString(R.string.message_downloading));
        TaskExecutor executor = new FileDownloadTask(downloadURL, modpack.toFile())
                .whenComplete(Schedulers.androidUIThread(), e -> {
                    if (e == null) {
                        LocalModpackPage page = new LocalModpackPage(context, PageManager.PAGE_ID_TEMP, parent, R.layout.page_modpack, profile, null, modpack.toFile());
                        DownloadPageManager.getInstance().showTempPage(page);
                    } else if (e instanceof CancellationException) {
                        Toast.makeText(context, context.getString(R.string.message_cancelled), Toast.LENGTH_SHORT).show();
                    } else {
                        FCLAlertDialog.Builder builder = new FCLAlertDialog.Builder(context);
                        builder.setAlertLevel(FCLAlertDialog.AlertLevel.ALERT);
                        builder.setCancelable(false);
                        builder.setTitle(context.getString(R.string.download_failed));
                        builder.setMessage(AndroidUtils.getLocalizedText(context, "install_failed_downloading_detail", file.getFile().getUrl()) + "\n" + StringUtils.getStackTrace(e));
                        builder.setNegativeButton(context.getString(com.tungsten.hfcl.R.string.dialog_positive), null);
                        builder.create().show();
                    }
                }).executor();
        taskDialog.setExecutor(executor);
        taskDialog.show();
        executor.start();
    }

    public static void deleteVersion(Context context, Profile profile, String version) {
        boolean isIndependent = profile.getVersionSetting(version).isIsolateGameDir();
        String message = isIndependent ? String.format(context.getString(R.string.version_manage_remove_confirm_independent), version) : String.format(context.getString(R.string.version_manage_remove_confirm), version);

        FCLAlertDialog.Builder builder = new FCLAlertDialog.Builder(context);
        builder.setAlertLevel(FCLAlertDialog.AlertLevel.ALERT);
        builder.setMessage(message);
        builder.setPositiveButton(() -> {
            ProgressDialog progress = new ProgressDialog(context);
            Task.runAsync(() -> {
                profile.getRepository().removeVersionFromDisk(version);
            }).whenComplete(Schedulers.androidUIThread(), (e) -> {
                progress.dismiss();
            }).start();
        });
        builder.setNegativeButton(null);
        builder.create().show();
    }

    public static CompletableFuture<String> renameVersion(Context context, Profile profile, String version) {
        RenameVersionDialog dialog = new RenameVersionDialog(context, version, (newName, resolve, reject) -> {
            if (!OperatingSystem.isNameValid(newName) || !ParseUtil.isValidCharacters(newName)) {
                reject.accept(context.getString(R.string.install_new_game_malformed));
                return;
            }
            ProgressDialog progress = new ProgressDialog(context);
            Task.supplyAsync(() -> profile.getRepository().renameVersion(version, newName))
                    .thenComposeAsync(Schedulers.androidUIThread(), result -> {
                        progress.dismiss();
                        if (result) {
                            resolve.run();
                            profile.getRepository().refreshVersionsAsync()
                                    .thenRunAsync(Schedulers.androidUIThread(), () -> {
                                        if (profile.getRepository().hasVersion(newName)) {
                                            profile.setSelectedVersion(newName);
                                        }
                                    }).start();
                        } else {
                            reject.accept(context.getString(R.string.version_manage_rename_fail));
                        }
                        return null;
                    }).start();
        });
        dialog.show();
        return dialog.getFuture();
    }

    public static void exportVersion(Context context, FCLUILayout parent, Profile profile, String version) {
        ModpackTypeSelectionPage page = new ModpackTypeSelectionPage(context, PageManager.PAGE_ID_TEMP, parent, R.layout.page_modpack_type, profile, version);
        ManagePageManager.getInstance().showTempPage(page);
    }

    public static void duplicateVersion(Context context, Profile profile, String version) {
        DuplicateVersionDialog dialog = new DuplicateVersionDialog(context, profile, version, (res, resolve, reject) -> {
            String newVersionName = (String) res.get(0);
            if (!OperatingSystem.isNameValid(newVersionName) || !ParseUtil.isValidCharacters(newVersionName)) {
                reject.accept(context.getString(R.string.install_new_game_malformed));
                return;
            }
            boolean copySaves = (boolean) res.get(1);
            ProgressDialog progress = new ProgressDialog(context);
            Task.runAsync(() -> profile.getRepository().duplicateVersion(version, newVersionName, copySaves))
                    .thenComposeAsync(profile.getRepository().refreshVersionsAsync())
                    .whenComplete(Schedulers.androidUIThread(), (result, exception) -> {
                        progress.dismiss();
                        if (exception == null) {
                            resolve.run();
                        } else {
                            reject.accept(StringUtils.getStackTrace(exception));
                            profile.getRepository().removeVersionFromDisk(newVersionName);
                        }
                    }).start();
        });
        dialog.show();
    }

    public static void updateVersion(Context context, FCLUILayout parent, Profile profile, String version) {
        ModpackSelectionPage page = new ModpackSelectionPage(context, PageManager.PAGE_ID_TEMP, parent, R.layout.page_modpack_selection, profile, version);
        ManagePageManager.getInstance().showTempPage(page);
    }

    public static void updateGameAssets(Context context, Profile profile, String version) {
        TaskExecutor executor = new GameAssetDownloadTask(profile.getDependency(), profile.getRepository().getVersion(version), GameAssetDownloadTask.DOWNLOAD_INDEX_FORCIBLY, true)
                .executor();
        TaskDialog dialog = new TaskDialog(context, TaskCancellationAction.NORMAL);
        dialog.setExecutor(executor);
        dialog.setTitle(context.getString(R.string.version_manage_redownload_assets_index));
        dialog.show();
        executor.start();
    }

    public static void cleanVersion(Profile profile, String id) {
        try {
            profile.getRepository().clean(id);
        } catch (IOException e) {
            Logging.LOG.log(Level.WARNING, "Unable to clean game directory", e);
        }
    }

    public static void launch(Context context, Profile profile) {
        launch(context, profile, profile.getSelectedVersion());
    }

    public static void launch(Context context, Profile profile, String id) {
        launch(context, profile, id, null);
    }

    public static void launch(Context context, Profile profile, String id, Consumer<LauncherHelper> injector) {
        if (!checkVersionForLaunching(context, profile, id))
            return;
        ensureSelectedAccount(context, account -> {
            LauncherHelper launcherHelper = new LauncherHelper(context, profile, account, id);
            if (injector != null)
                injector.accept(launcherHelper);
            launcherHelper.launch();
        });
    }

    private static boolean checkVersionForLaunching(Context context, Profile profile, String id) {
        if (id == null || !profile.getRepository().isLoaded() || !profile.getRepository().hasVersion(id)) {
            FCLAlertDialog.Builder builder = new FCLAlertDialog.Builder(context);
            builder.setCancelable(false);
            builder.setAlertLevel(FCLAlertDialog.AlertLevel.ALERT);
            builder.setTitle(context.getString(R.string.launch_failed));
            builder.setMessage(context.getString(R.string.version_empty_launch));
            builder.setNegativeButton(context.getString(com.tungsten.hfcl.R.string.dialog_positive), () -> {
                MainActivity.getInstance().refreshMenuView(null);
                MainActivity.getInstance().binding.download.setSelected(true);
            });
            builder.create().show();
            return false;
        } else {
            return true;
        }
    }

    private static void ensureSelectedAccount(Context context, Consumer<Account> action) {
        Account account = Accounts.getSelectedAccount();
        if (account == null) {
            CreateAccountDialog dialog = new CreateAccountDialog(context, (AccountFactory<?>) null);
            dialog.setOnDismissListener(dialogInterface -> {
                Account newAccount = Accounts.getSelectedAccount();
                if (newAccount == null) {
                    // user cancelled operation
                } else {
                    action.accept(newAccount);
                }
            });
            dialog.show();
        } else {
            action.accept(account);
        }
    }

}
