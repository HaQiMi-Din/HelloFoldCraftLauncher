package com.tungsten.hfcl.control.view;

import com.tungsten.hfcl.control.data.CustomControl;

public interface CustomView {
    CustomControl.ViewType getType();
    String getViewId();
    void switchParentVisibility();
    void removeListener();
}
