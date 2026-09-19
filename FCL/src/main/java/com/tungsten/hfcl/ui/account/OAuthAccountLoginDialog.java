package com.tungsten.hfcl.ui.account;

import static com.tungsten.hfclcore.util.Logging.LOG;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import com.tungsten.hfcl.R;
import com.tungsten.hfcl.game.OAuthServer;
import com.tungsten.hfcl.setting.Accounts;
import com.tungsten.hfcl.util.AndroidUtils;
import com.tungsten.hfcl.util.FXUtils;
import com.tungsten.hfcl.util.WeakListenerHolder;
import com.tungsten.hfclcore.auth.AuthInfo;
import com.tungsten.hfclcore.auth.OAuthAccount;
import com.tungsten.hfclcore.fakefx.beans.property.ObjectProperty;
import com.tungsten.hfclcore.fakefx.beans.property.SimpleObjectProperty;
import com.tungsten.hfclcore.task.Schedulers;
import com.tungsten.hfclcore.task.Task;
import com.tungsten.hfcllibrary.component.dialog.FCLAlertDialog;
import com.tungsten.hfcllibrary.component.dialog.FCLDialog;
import com.tungsten.hfcllibrary.component.view.FCLButton;

import java.util.function.Consumer;
import java.util.logging.Level;

public class OAuthAccountLoginDialog extends FCLDialog implements View.OnClickListener {

    private final FCLButton positive;
    private final FCLButton negative;

    private final OAuthAccount account;
    private final Consumer<AuthInfo> success;
    private final Runnable failed;
    private final ObjectProperty<OAuthServer.GrantDeviceCodeEvent> deviceCode = new SimpleObjectProperty<>();

    private final WeakListenerHolder holder = new WeakListenerHolder();
    private boolean useExternalBrowser = false;

    public OAuthAccountLoginDialog(@NonNull Context context, OAuthAccount account, Consumer<AuthInfo> success, Runnable failed) {
        super(context);
        this.account = account;
        this.success = success;
        this.failed = failed;

        setContentView(R.layout.dialog_relogin_oauth);
        setCancelable(false);

        FXUtils.onChangeAndOperate(deviceCode, deviceCode -> Schedulers.androidUIThread().execute(() -> {
            if (deviceCode != null) {
                AndroidUtils.copyText(getContext(), deviceCode.getUserCode());
            }
        }));
        holder.add(Accounts.OAUTH_CALLBACK.onGrantDeviceCode.registerWeak(deviceCode::set));
        holder.add(Accounts.OAUTH_CALLBACK.onOpenBrowser.registerWeak(event -> {
            if (useExternalBrowser) {
                AndroidUtils.openLink(context, event.getUrl());
            } else {
                AndroidUtils.openLinkWithBuiltinWebView(context, event.getUrl());
            }
        }));

        positive = findViewById(R.id.login);
        negative = findViewById(R.id.cancel);

        positive.setOnClickListener(this);
        negative.setOnClickListener(this);

        positive.setOnLongClickListener(view -> {
            useExternalBrowser = true;
            onClick(positive);
            return true;
        });
    }

    @Override
    public void onClick(View view) {
        if (view == positive) {
            positive.setEnabled(false);
            negative.setEnabled(false);
            Task.supplyAsync(account::logInWhenCredentialsExpired)
                    .whenComplete(Schedulers.androidUIThread(), (authInfo, exception) -> {
                        if (exception == null) {
                            success.accept(authInfo);
                            dismiss();
                        } else {
                            LOG.log(Level.INFO, "Failed to login when credentials expired: " + account, exception);
                            FCLAlertDialog.Builder builder = new FCLAlertDialog.Builder(getContext());
                            builder.setAlertLevel(FCLAlertDialog.AlertLevel.ALERT);
                            builder.setMessage(Accounts.localizeErrorMessage(getContext(), exception));
                            builder.setCancelable(false);
                            builder.setNegativeButton(getContext().getString(com.tungsten.hfcl.R.string.dialog_positive), null);
                            builder.create().show();
                        }
                        positive.setEnabled(true);
                        negative.setEnabled(true);
                    }).start();
        }
        if (view == negative) {
            failed.run();
            dismiss();
        }
    }
}
