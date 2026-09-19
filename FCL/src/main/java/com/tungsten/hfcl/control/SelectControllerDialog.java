package com.tungsten.hfcl.control;

import static com.tungsten.hfcl.util.FXUtils.onInvalidating;

import android.content.Context;
import android.view.View;
import android.widget.ListView;

import androidx.annotation.NonNull;

import com.tungsten.hfcl.R;
import com.tungsten.hfcl.setting.Controller;
import com.tungsten.hfcl.setting.Controllers;
import com.tungsten.hfclcore.fakefx.beans.property.ObjectProperty;
import com.tungsten.hfclcore.fakefx.beans.property.SimpleObjectProperty;
import com.tungsten.hfcllibrary.component.dialog.FCLDialog;
import com.tungsten.hfcllibrary.component.view.FCLButton;

public class SelectControllerDialog extends FCLDialog implements View.OnClickListener {

    private final Callback callback;

    private ListView listView;
    private FCLButton positive;

    private final ObjectProperty<Controller> selectedController = new SimpleObjectProperty<Controller>() {
        {
            Controllers.getControllers().addListener(onInvalidating(this::invalidated));
        }

        @Override
        protected void invalidated() {
            if (!Controllers.isInitialized()) return;

            Controller controller = get();
            if (Controllers.getControllers().isEmpty()) {
                if (controller != null) {
                    set(null);
                }
            } else {
                if (!Controllers.getControllers().contains(controller)) {
                    set(Controllers.getControllers().get(0));
                }
            }
        }
    };

    public Controller getSelectedController() {
        return selectedController.get();
    }

    public void setSelectedController(Controller selectedController) {
        this.selectedController.set(selectedController);
    }

    public SelectControllerDialog(@NonNull Context context, String id, Callback callback) {
        super(context);
        this.callback = callback;
        setContentView(R.layout.dialog_select_controller);
        setCancelable(false);
        listView = findViewById(R.id.list);
        positive = findViewById(R.id.positive);
        positive.setOnClickListener(this);

        boolean set = true;
        for (Controller controller : Controllers.getControllers()) {
            if (controller.getId().equals(id)) {
                setSelectedController(controller);
                set = false;
            }
        }
        if (set) {
            setSelectedController(null);
        }

        refreshList();
    }

    public void refreshList() {
        SelectableControllerListAdapter adapter = new SelectableControllerListAdapter(getContext(), Controllers.controllersProperty(), this);
        listView.setAdapter(adapter);
    }

    @Override
    public void onClick(View view) {
        if (view == positive) {
            callback.onControllerSelected(selectedController.get());
            dismiss();
        }
    }

    public interface Callback {
        void onControllerSelected(Controller controller);
    }
}
