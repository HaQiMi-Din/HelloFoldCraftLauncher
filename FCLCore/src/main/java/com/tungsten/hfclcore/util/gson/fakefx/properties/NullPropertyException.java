package com.tungsten.hfclcore.util.gson.fakefx.properties;

public class NullPropertyException extends RuntimeException {

    public NullPropertyException() {
        super("Null properties are forbidden");
    }
}