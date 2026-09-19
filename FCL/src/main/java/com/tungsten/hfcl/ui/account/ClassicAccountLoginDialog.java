package com.tungsten.hfcl.ui.account;

import android.content.Context;

import androidx.annotation.NonNull;

import com.tungsten.hfclcore.auth.AuthInfo;
import com.tungsten.hfclcore.auth.ClassicAccount;
import com.tungsten.hfcllibrary.component.dialog.FCLDialog;

import java.util.function.Consumer;

public class ClassicAccountLoginDialog extends FCLDialog {
    public ClassicAccountLoginDialog(@NonNull Context context, ClassicAccount oldAccount, Consumer<AuthInfo> success, Runnable failed) {
        super(context);
    }
}
