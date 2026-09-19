package com.tungsten.hfcl.ui

import android.content.Context
import com.tungsten.hfcl.R
import com.tungsten.hfcl.ui.account.AccountUI
import com.tungsten.hfcl.ui.controller.ControllerUI
import com.tungsten.hfcl.ui.download.DownloadUI
import com.tungsten.hfcl.ui.main.MainUI
import com.tungsten.hfcl.ui.manage.ManageUI
import com.tungsten.hfcl.ui.multiplayer.MultiplayerUI
import com.tungsten.hfcl.ui.setting.SettingUI
import com.tungsten.hfcl.ui.version.VersionUI
import com.tungsten.hfclcore.util.Logging
import com.tungsten.hfcllibrary.component.ui.FCLBaseUI
import com.tungsten.hfcllibrary.component.ui.FCLCommonUI
import com.tungsten.hfcllibrary.component.view.FCLUILayout
import java.util.logging.Level

class UIManager(val context: Context, val parent: FCLUILayout) {
    companion object {
        @JvmStatic
        lateinit var instance: UIManager
    }

    private var initialized = false
    lateinit var mainUI: MainUI
    val accountUI: AccountUI by lazy { AccountUI(context, parent, R.layout.ui_account) }
    val versionUI: VersionUI by lazy { VersionUI(context, parent, R.layout.ui_version) }
    val manageUI: ManageUI by lazy { ManageUI(context, parent, R.layout.ui_manage) }
    val downloadUI: DownloadUI by lazy { DownloadUI(context, parent, R.layout.ui_download) }
    val controllerUI: ControllerUI by lazy { ControllerUI(context, parent, R.layout.ui_controller) }
    val multiplayerUI: MultiplayerUI by lazy { MultiplayerUI(context, parent, R.layout.ui_multiplayer) }
    val settingUI: SettingUI by lazy { SettingUI(context, parent, R.layout.ui_setting) }

    private val allUIList = mutableListOf<FCLBaseUI>()
    var currentUI: FCLBaseUI? = null

    fun init() {
        if (initialized) {
            Logging.LOG.log(Level.WARNING, "UIManager already initialized!")
            return
        }
        instance = this
        mainUI = MainUI(context, parent, R.layout.ui_main)
        allUIList.add(mainUI)
    }

    fun switchUI(ui: FCLCommonUI) {
        if (!allUIList.contains(ui)) {
            allUIList.add(ui)
        }
        for (baseUI in allUIList) {
            if (ui === baseUI) {
                currentUI?.onStop()
                ui.onStart()
                currentUI = ui
                break
            }
        }
    }

    fun registerDefaultBackEvent(runnable: Runnable?) {
        FCLBaseUI.setDefaultBackEvent(runnable)
    }

    fun onBackPressed() {
        currentUI?.onBackPressed()
    }

    fun onPause() {
        for (baseUI in allUIList) {
            baseUI.onPause()
        }
    }

    fun onResume() {
        for (baseUI in allUIList) {
            baseUI.onResume()
        }
    }
}