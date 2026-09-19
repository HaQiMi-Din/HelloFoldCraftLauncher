package com.tungsten.hfcl.ui.manage

import android.content.Context
import android.content.res.ColorStateList
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import com.mio.util.AnimUtil
import com.mio.util.AnimUtil.Companion.interpolator
import com.mio.util.showErrorDialog
import com.tungsten.hfcl.R
import com.tungsten.hfcl.databinding.PageManageVersionBinding
import com.tungsten.hfcl.setting.Profile
import com.tungsten.hfcl.ui.UIManager.Companion.instance
import com.tungsten.hfcl.ui.manage.ManageUI.VersionLoadable
import com.tungsten.hfcl.ui.manage.adapter.ManageItemAdapter
import com.tungsten.hfcl.ui.manage.item.ManageItem
import com.tungsten.hfcl.ui.version.Versions
import com.tungsten.hfcl.util.RequestCodes
import com.tungsten.hfclauncher.utils.FCLPath
import com.tungsten.hfclcore.fakefx.beans.property.BooleanProperty
import com.tungsten.hfclcore.fakefx.beans.property.SimpleBooleanProperty
import com.tungsten.hfclcore.task.Schedulers
import com.tungsten.hfclcore.task.Task
import com.tungsten.hfclcore.util.io.FileUtils
import com.tungsten.hfcllibrary.browser.FileBrowser
import com.tungsten.hfcllibrary.browser.options.LibMode
import com.tungsten.hfcllibrary.component.dialog.FCLAlertDialog
import com.tungsten.hfcllibrary.component.theme.ThemeEngine
import com.tungsten.hfcllibrary.component.ui.FCLCommonPage
import com.tungsten.hfcllibrary.component.view.FCLUILayout
import com.tungsten.hfcllibrary.ui.ProgressDialog
import com.tungsten.hfcllibrary.util.uploadLog
import java.io.File

class ManagePage(context: Context, id: Int, parent: FCLUILayout, resId: Int) :
    FCLCommonPage(context, id, parent, resId), VersionLoadable {
    private val currentVersionUpgradable: BooleanProperty = SimpleBooleanProperty()
    val profile: Profile
        get() = instance.manageUI.profile
    val version: String
        get() = instance.manageUI.version

    private lateinit var binding: PageManageVersionBinding

    init {
        create()
    }

    override fun refresh(vararg param: Any): Task<*>? {
        return null
    }

    override fun loadVersion(profile: Profile, version: String) {
        currentVersionUpgradable.set(profile.repository.isModpack(version))
    }

    private fun create() {
        binding = PageManageVersionBinding.bind(contentView).apply {
            ThemeEngine.getInstance().registerEvent(left) {
                left.backgroundTintList = ColorStateList(
                    arrayOf(intArrayOf()), intArrayOf(ThemeEngine.getInstance().getTheme().ltColor)
                )
            }
            ThemeEngine.getInstance().registerEvent(right) {
                right.backgroundTintList = ColorStateList(
                    arrayOf(intArrayOf()), intArrayOf(ThemeEngine.getInstance().getTheme().ltColor)
                )
            }

            left.layoutManager = LinearLayoutManager(context)
            left.adapter = ManageItemAdapter(
                context,
                listOf(
                    ManageItem(R.drawable.ic_baseline_cloud_upload_24, R.string.upload_log) {
                        uploadLatestLog()
                    },
                    ManageItem(R.drawable.ic_baseline_script_24, R.string.folder_fcl_log) {
                        onBrowse(
                            FCLPath.LOG_DIR
                        )
                    },
                    ManageItem(R.drawable.ic_baseline_videogame_asset_24, R.string.folder_game) {
                        onBrowse("")
                    },
                    ManageItem(R.drawable.ic_outline_extension_24, R.string.folder_mod) {
                        onBrowse("mods")
                    },
                    ManageItem(R.drawable.ic_baseline_settings_24, R.string.folder_config) {
                        onBrowse("config")
                    },
                    ManageItem(R.drawable.ic_baseline_texture_24, R.string.folder_resourcepacks) {
                        onBrowse("resourcepacks")
                    },
                    ManageItem(R.drawable.ic_baseline_application_24, R.string.folder_shaderpacks) {
                        onBrowse("shaderpacks")
                    },
                    ManageItem(R.drawable.ic_baseline_screenshot_24, R.string.folder_screenshots) {
                        onBrowse("screenshots")
                    },
                    ManageItem(R.drawable.ic_baseline_earth_24, R.string.folder_saves) {
                        onBrowse("saves")
                    }

                ))
            right.layoutManager = LinearLayoutManager(context)
            right.adapter = ManageItemAdapter(
                context,
                listOf(
                    ManageItem(R.drawable.ic_baseline_update_24, R.string.version_update) {
                        if (!currentVersionUpgradable.get()) {
                            AnimUtil.playTranslationX(it, 500, 0f, 50f, -50f, 0f)
                                .interpolator(OvershootInterpolator()).start()
                        } else {
                            updateGame()
                        }
                    },
                    ManageItem(R.drawable.ic_baseline_edit_24, R.string.version_manage_rename) {
                        rename()
                    },
                    ManageItem(
                        R.drawable.ic_baseline_content_copy_24,
                        R.string.version_manage_duplicate
                    ) {
                        duplicate()
                    },
                    ManageItem(R.drawable.ic_baseline_output_24, R.string.modpack_export) {
                        export()
                    },
                    ManageItem(
                        R.drawable.ic_baseline_list_24,
                        R.string.version_manage_redownload_assets_index
                    ) {
                        redownloadAssetIndex()
                    },
                    ManageItem(
                        R.drawable.ic_baseline_delete_24,
                        R.string.version_manage_remove_libraries
                    ) {
                        clearLibraries()
                    },
                    ManageItem(
                        R.drawable.ic_baseline_delete_24,
                        R.string.version_manage_clean
                    ) {
                        clearJunkFiles()
                    }
                ))
        }
    }

    private fun onBrowse(path: String) {
        val root =
            if (path.startsWith("/")) File(path) else if (path.isEmpty()) profile.repository.getRunDirectory(
                version
            ) else File(
                profile.repository.getRunDirectory(version), path
            )
        if (!root.exists()) {
            root.mkdirs()
        }
        FileBrowser.Builder(context)
            .setInitDir(root.absolutePath)
            .setLibMode(LibMode.FILE_BROWSER)
            .create()
            .browse(activity)
    }

    private fun redownloadAssetIndex() {
        Versions.updateGameAssets(context, profile, version)
    }

    private fun clearLibraries() {
        val builder = FCLAlertDialog.Builder(context)
        builder.setAlertLevel(FCLAlertDialog.AlertLevel.ALERT)
        builder.setMessage(
            String.format(
                context.getString(R.string.version_manage_remove_confirm),
                "libraries"
            )
        )
        builder.setPositiveButton {
            val progress = ProgressDialog(context)
            Task.runAsync {
                FileUtils.deleteDirectoryQuietly(
                    File(
                        profile.repository.baseDirectory, "libraries"
                    )
                )
            }.whenComplete(Schedulers.androidUIThread()) { _: Exception? ->
                progress.dismiss()
            }.start()
        }
        builder.setNegativeButton(null)
        builder.create().show()
    }

    private fun clearJunkFiles() {
        val builder = FCLAlertDialog.Builder(context)
        builder.setAlertLevel(FCLAlertDialog.AlertLevel.ALERT)
        builder.setMessage(
            String.format(
                context.getString(R.string.version_manage_remove_confirm),
                "logs"
            )
        )
        builder.setPositiveButton {
            val progress = ProgressDialog(context)
            Task.runAsync {
                Versions.cleanVersion(
                    profile, version
                )
            }.whenComplete(Schedulers.androidUIThread()) { _: Exception? ->
                progress.dismiss()
            }.start()
        }
        builder.setNegativeButton(null)
        builder.create().show()
    }

    private fun updateGame() {
        Versions.updateVersion(context, parent, profile, version)
    }

    private fun export() {
        Versions.exportVersion(context, parent, profile, version)
    }

    private fun rename() {
        Versions.renameVersion(context, profile, version)
            .thenApply {
                instance.manageUI.preferredVersionName = it
            }
    }

    private fun duplicate() {
        Versions.duplicateVersion(context, profile, version)
    }

    private fun uploadLatestLog() {
        val logFile = File(FCLPath.LOG_DIR, "latest_game.log")
        if (!logFile.exists()) {
            showErrorDialog(context,R.string.log_not_found)
            return
        }
        try {
            if (logFile.length() > 5 * 1024 * 1024) {
                showErrorDialog(context,R.string.log_too_large)
                return
            }
            val logs = FileUtils.readText(logFile)
            uploadLog(activity, logs)
        } catch (e: Exception) {
            showErrorDialog(context,"Failed to read log: ${e.message}")
        }
    }

}
