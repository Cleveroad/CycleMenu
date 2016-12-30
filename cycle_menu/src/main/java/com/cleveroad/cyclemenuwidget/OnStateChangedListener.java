package com.cleveroad.cyclemenuwidget;

/**
 * Callback on changed stet in cycle menu.
 */
public interface OnStateChangedListener {

    void onStateChanged(CycleMenuWidget.STATE state);

    void onOpenComplete();

    void onCloseComplete();

}
