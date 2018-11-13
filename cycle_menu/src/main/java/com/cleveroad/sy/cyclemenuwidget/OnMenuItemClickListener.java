package com.cleveroad.sy.cyclemenuwidget;

import android.view.View;

/**
 * Listener for menu items clicks
 */
public interface OnMenuItemClickListener {
    void onMenuItemClick(View view, int itemPosition);
    void onMenuItemLongClick(View view, int itemPosition);
}
