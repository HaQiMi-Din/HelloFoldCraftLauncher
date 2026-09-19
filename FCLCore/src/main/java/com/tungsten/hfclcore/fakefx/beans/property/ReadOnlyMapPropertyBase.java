package com.tungsten.hfclcore.fakefx.beans.property;

import com.tungsten.hfclcore.fakefx.beans.InvalidationListener;
import com.tungsten.hfclcore.fakefx.beans.value.ChangeListener;
import com.tungsten.hfclcore.fakefx.binding.MapExpressionHelper;
import com.tungsten.hfclcore.fakefx.collections.MapChangeListener;
import com.tungsten.hfclcore.fakefx.collections.ObservableMap;

/**
 * Base class for all readonly properties wrapping an {@link ObservableMap}.
 * This class provides a default implementation to attach listener.
 *
 * @see ReadOnlyMapProperty
 * @since JavaFX 2.1
 */
public abstract class ReadOnlyMapPropertyBase<K, V> extends ReadOnlyMapProperty<K, V> {

    private MapExpressionHelper<K, V> helper;

    /**
     * Creates a default {@code ReadOnlyMapPropertyBase}.
     */
    public ReadOnlyMapPropertyBase() {
    }

    @Override
    public void addListener(InvalidationListener listener) {
        helper = MapExpressionHelper.addListener(helper, this, listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        helper = MapExpressionHelper.removeListener(helper, listener);
    }

    @Override
    public void addListener(ChangeListener<? super ObservableMap<K, V>> listener) {
        helper = MapExpressionHelper.addListener(helper, this, listener);
    }

    @Override
    public void removeListener(ChangeListener<? super ObservableMap<K, V>> listener) {
        helper = MapExpressionHelper.removeListener(helper, listener);
    }

    @Override
    public void addListener(MapChangeListener<? super K, ? super V> listener) {
        helper = MapExpressionHelper.addListener(helper, this, listener);
    }

    @Override
    public void removeListener(MapChangeListener<? super K, ? super V> listener) {
        helper = MapExpressionHelper.removeListener(helper, listener);
    }

    protected void fireValueChangedEvent() {
        MapExpressionHelper.fireValueChangedEvent(helper);
    }

    protected void fireValueChangedEvent(MapChangeListener.Change<? extends K, ? extends V> change) {
        MapExpressionHelper.fireValueChangedEvent(helper, change);
    }



}
