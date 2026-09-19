package com.tungsten.hfcllibrary.browser.adapter;

public interface FileBrowserListener {
    void onEnterDir(String path);
    void onSelect(FileBrowserAdapter adapter, String path);
}
