package com.cleveroad.sy.cyclemenuwidget;

/**
 * StateSaveListener allow you to handle saving cycleMenu state when the view will be detached from its parent.
 * It send to the listener position from adapter of first item and offset angle of items in degrees.
 */
public interface StateSaveListener {

    /**
     * Send to listener menu position and angle offset.
     * @param itemPosition  position of the first item in adapter
     * @param lastItemAngleShift shift angle of items in menu
     */
    void saveState(int itemPosition, double lastItemAngleShift);

}
