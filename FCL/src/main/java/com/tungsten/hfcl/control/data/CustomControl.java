package com.tungsten.hfcl.control.data;

public interface CustomControl {

    enum ViewType {
        CONTROL_BUTTON,
        CONTROL_DIRECTION
    }

    ViewType getType();
    String getViewId();
    CustomControl cloneView();
}
