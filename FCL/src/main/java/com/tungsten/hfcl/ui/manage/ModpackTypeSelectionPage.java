package com.tungsten.hfcl.ui.manage;

import android.content.Context;
import android.view.View;

import com.tungsten.hfcl.R;
import com.tungsten.hfcl.setting.Profile;
import com.tungsten.hfcl.ui.PageManager;
import com.tungsten.hfclcore.mod.ModpackExportInfo;
import com.tungsten.hfclcore.mod.mcbbs.McbbsModpackExportTask;
import com.tungsten.hfclcore.mod.multimc.MultiMCModpackExportTask;
import com.tungsten.hfclcore.mod.server.ServerModpackExportTask;
import com.tungsten.hfclcore.task.Task;
import com.tungsten.hfcllibrary.component.ui.FCLTempPage;
import com.tungsten.hfcllibrary.component.view.FCLLinearLayout;
import com.tungsten.hfcllibrary.component.view.FCLUILayout;

public class ModpackTypeSelectionPage extends FCLTempPage implements View.OnClickListener {

    private final Profile profile;
    private final String version;

    private FCLLinearLayout mcbbs;
    private FCLLinearLayout multimc;
    private FCLLinearLayout server;

    public ModpackTypeSelectionPage(Context context, int id, FCLUILayout parent, int resId, Profile profile, String version) {
        super(context, id, parent, resId);
        this.profile = profile;
        this.version = version;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mcbbs = findViewById(R.id.mcbbs);
        multimc = findViewById(R.id.multimc);
        server = findViewById(R.id.server);
        mcbbs.setOnClickListener(this);
        multimc.setOnClickListener(this);
        server.setOnClickListener(this);
    }

    @Override
    public Task<?> refresh(Object... param) {
        return null;
    }

    @Override
    public void onRestart() {

    }

    @Override
    public void onClick(View v) {
        String type = null;
        ModpackExportInfo.Options options = null;
        if (v == mcbbs) {
            type = MODPACK_TYPE_MCBBS;
            options = McbbsModpackExportTask.OPTION;
        }
        if (v == multimc) {
            type = MODPACK_TYPE_MULTIMC;
            options = MultiMCModpackExportTask.OPTION;
        }
        if (v == server) {
            type = MODPACK_TYPE_SERVER;
            options = ServerModpackExportTask.OPTION;
        }
        ModpackInfoPage page = new ModpackInfoPage(getContext(), PageManager.PAGE_ID_TEMP, getParent(), R.layout.page_modpack_info, profile, version, type, options);
        ManagePageManager.getInstance().showTempPage(page);
    }

    public static final String MODPACK_TYPE_MCBBS = "mcbbs";
    public static final String MODPACK_TYPE_MULTIMC = "multimc";
    public static final String MODPACK_TYPE_SERVER = "server";
}
