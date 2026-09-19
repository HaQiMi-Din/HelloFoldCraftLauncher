package com.tungsten.hfcllibrary.ui;

import android.content.Context;

import androidx.annotation.NonNull;

import com.tungsten.hfcllibrary.component.dialog.FCLDialog;
import com.tungsten.hfcllibrary.component.view.FCLProgressBar;

public class ProgressDialog extends FCLDialog {
    public ProgressDialog(@NonNull Context context) {
        super(context);
        setCancelable(false);
        setCanceledOnTouchOutside(false);
        FCLProgressBar progressBar = new FCLProgressBar(context);
        setContentView(progressBar);
        show();
    }
}
