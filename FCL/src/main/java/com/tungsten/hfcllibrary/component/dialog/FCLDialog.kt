package com.tungsten.hfcllibrary.component.dialog

import android.content.Context
import androidx.appcompat.app.AppCompatDialog
import com.tungsten.hfcl.R
import com.tungsten.hfcllibrary.component.theme.ThemeEngine

open class FCLDialog(context: Context) : AppCompatDialog(context) {
    init {
        ThemeEngine.getInstance()
            .applyFullscreen(window, ThemeEngine.getInstance().getTheme().isFullscreen)
        window?.setBackgroundDrawableResource(R.drawable.dialog_background)
    }
}
