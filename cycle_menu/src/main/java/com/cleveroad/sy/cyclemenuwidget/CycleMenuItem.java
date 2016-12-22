package com.cleveroad.sy.cyclemenuwidget;

import android.graphics.drawable.Drawable;

/**
 * Model class for menu items
 */
public class CycleMenuItem {
    private Drawable mIcon;
    private int mId;

    public CycleMenuItem(int id, Drawable icon) {
        mId = id;
        mIcon = icon;
    }

    public int getId() {
        return mId;
    }

    public Drawable getIcon() {
        return mIcon;
    }

}
