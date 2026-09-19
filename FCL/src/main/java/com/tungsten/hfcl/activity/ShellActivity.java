package com.tungsten.hfcl.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;

import com.tungsten.hfcl.R;
import com.tungsten.hfcl.control.view.LogWindow;
import com.tungsten.hfcl.util.ShellUtil;
import com.tungsten.hfclauncher.utils.FCLPath;
import com.tungsten.hfcllibrary.component.FCLActivity;
import com.tungsten.hfcllibrary.component.theme.Theme;
import com.tungsten.hfcllibrary.component.view.FCLEditText;

import java.io.File;
import java.util.Objects;

public class ShellActivity extends FCLActivity {

    private EditText logWindow;
    private FCLEditText editText;
    private ShellUtil shellUtil;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shell);
        logWindow = findViewById(R.id.shell_log_window);
        editText = findViewById(R.id.shell_input);
        appendLog("Welcome to use Fold Craft Launcher!\n");
        appendLog("Here is the shell command line!\n");
        shellUtil = new ShellUtil(new File(FCLPath.FILES_DIR).getParent(), output -> runOnUiThread(() -> appendLog("\t" + output + "\n")));
        shellUtil.start();
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String cmd = Objects.requireNonNull(editText.getText()).toString();
                if (cmd.endsWith("\n")) {
                    appendLog("->" + cmd);
                    editText.setText("");
                    if (cmd.contains("clear")) {
                        logWindow.setText("");
                        return;
                    }
                    shellUtil.append(cmd);
                }
            }
        });
        logWindow.setOnClickListener(v -> {
            editText.requestFocus();
        });
        logWindow.setKeyListener(new KeyListener() {
            @Override
            public int getInputType() {
                return 0;
            }

            @Override
            public boolean onKeyDown(View view, Editable text, int keyCode, KeyEvent event) {
                return true;
            }

            @Override
            public boolean onKeyUp(View view, Editable text, int keyCode, KeyEvent event) {
                return true;
            }

            @Override
            public boolean onKeyOther(View view, Editable text, KeyEvent event) {
                return true;
            }

            @Override
            public void clearMetaKeyState(View view, Editable content, int states) {

            }
        });
    }

    private void appendLog(String str) {
        logWindow.append(str);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        shellUtil.interrupt();
    }
}
