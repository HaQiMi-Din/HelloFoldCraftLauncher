package com.tungsten.hfclcore.fakefx.beans.value;

import com.tungsten.hfclcore.fakefx.collections.ObservableMap;

public interface WritableMapValue<K, V> extends WritableObjectValue<ObservableMap<K,V>>, ObservableMap<K, V> {
}
