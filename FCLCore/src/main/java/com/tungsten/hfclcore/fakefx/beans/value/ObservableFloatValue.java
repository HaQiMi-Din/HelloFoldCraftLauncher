package com.tungsten.hfclcore.fakefx.beans.value;

/**
 * An observable float value.
 *
 * @see ObservableValue
 * @see ObservableNumberValue
 *
 *
 * @since JavaFX 2.0
 */
public interface ObservableFloatValue extends ObservableNumberValue {

    /**
     * Returns the current value of this {@code ObservableFloatValue}.
     *
     * @return The current value
     */
    float get();
}
