package com.tungsten.hfclcore.util.gson.fakefx.creators;

import com.google.gson.InstanceCreator;
import com.tungsten.hfclcore.fakefx.collections.FXCollections;
import com.tungsten.hfclcore.fakefx.collections.ObservableList;

import java.lang.reflect.Type;

public class ObservableListCreator implements InstanceCreator<ObservableList<?>> {

    public ObservableList<?> createInstance(Type type) {
        // No need to use a parametrized list since the actual instance will have the raw type anyway.
        return FXCollections.observableArrayList();
    }
}