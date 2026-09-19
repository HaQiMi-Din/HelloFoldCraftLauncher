package com.tungsten.hfcl.ui.manage;

import android.content.Context;
import android.view.View;
import android.widget.ListView;

import androidx.annotation.NonNull;

import com.tungsten.hfcl.R;
import com.tungsten.hfclcore.mod.LocalModFile;
import com.tungsten.hfcllibrary.component.dialog.FCLDialog;
import com.tungsten.hfcllibrary.component.view.FCLButton;

import java.util.List;

public class ModRollbackDialog extends FCLDialog implements View.OnClickListener {

    private ListView listView;

    private FCLButton negative;

    public ModRollbackDialog(@NonNull Context context, List<LocalModFile> list, Callback callback) {
        super(context);
        setCancelable(false);
        setContentView(R.layout.dialog_rollback_mod);

        listView = findViewById(R.id.list);
        negative = findViewById(R.id.negative);
        negative.setOnClickListener(this);

        ModOldVersionListAdapter adapter = new ModOldVersionListAdapter(getContext(), this, list, callback);
        listView.setAdapter(adapter);
    }

    @Override
    public void onClick(View v) {
        if (v == negative) {
            dismiss();
        }
    }

    public interface Callback {
        void onOldVersionSelect(LocalModFile localModFile);
    }
}
