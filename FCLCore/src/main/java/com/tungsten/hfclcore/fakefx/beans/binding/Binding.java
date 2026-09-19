package com.tungsten.hfclcore.fakefx.beans.binding;

import com.tungsten.hfclcore.fakefx.beans.value.ObservableValue;
import com.tungsten.hfclcore.fakefx.collections.ObservableList;

public interface Binding<T> extends ObservableValue<T> {

    boolean isValid();

    void invalidate();

    ObservableList<?> getDependencies();

    void dispose();

}
