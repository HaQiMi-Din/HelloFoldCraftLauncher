package com.tungsten.hfcllibrary.component.ui;

import java.util.ArrayList;

public interface FCLMultiPageUICallback {
    void initPages();
    ArrayList<FCLBasePage> getAllPages();
    FCLBasePage getPage(int id);
}
