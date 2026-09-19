package com.tungsten.hfclauncher.bridge;

public interface FCLBridgeCallback {

    void onCursorModeChange(int mode);
    void onLog(String log);
    void onExit(int code);

}
