package com.tungsten.hfcl.ui.version;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.tungsten.hfcl.R;
import com.tungsten.hfcl.activity.MainActivity;
import com.tungsten.hfcl.setting.Profile;
import com.tungsten.hfcl.setting.Profiles;
import com.tungsten.hfclcore.util.StringUtils;
import com.tungsten.hfcllibrary.component.dialog.FCLDialog;
import com.tungsten.hfcllibrary.component.view.FCLButton;
import com.tungsten.hfcllibrary.component.view.FCLEditText;
import com.tungsten.hfcllibrary.component.view.FCLImageButton;
import com.tungsten.hfcllibrary.component.view.FCLTextView;

import java.io.File;

public class AddProfileDialog extends FCLDialog implements View.OnClickListener {

    private final FCLEditText editText;
    private final FCLTextView pathText;
    private final FCLImageButton editPath;
    private final FCLButton positive;
    private final FCLButton negative;

    public AddProfileDialog(@NonNull Context context) {
        super(context);
        setContentView(R.layout.dialog_add_profile);
        setCancelable(false);
        editText = findViewById(R.id.name);
        pathText = findViewById(R.id.path);
        editPath = findViewById(R.id.edit);
        positive = findViewById(R.id.positive);
        negative = findViewById(R.id.negative);
        editPath.setOnClickListener(this);
        positive.setOnClickListener(this);
        negative.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view == editPath) {
            MainActivity.getInstance().fileLauncher.launchSingleSelection(null, null, true, files -> {
                if (files == null) return;
                pathText.setText(files.get(0));
            });
        }
        if (view == positive) {
            if (StringUtils.isBlank(editText.getText().toString()) || StringUtils.isBlank(pathText.getText().toString())) {
                Toast.makeText(getContext(), getContext().getString(R.string.input_not_empty), Toast.LENGTH_SHORT).show();
            } else if (Profiles.getProfiles().stream().anyMatch(profile -> profile.getName().equals(editText.getText().toString()))) {
                Toast.makeText(getContext(), getContext().getString(R.string.profile_already_exist), Toast.LENGTH_SHORT).show();
            } else {
                Profiles.getProfiles().add(new Profile(editText.getText().toString(), new File(pathText.getText().toString())));
                ((VersionListPage) VersionPageManager.getInstance().getAllPages().get(0)).refreshProfile();
                dismiss();
            }
        }
        if (view == negative) {
            dismiss();
        }
    }
}
