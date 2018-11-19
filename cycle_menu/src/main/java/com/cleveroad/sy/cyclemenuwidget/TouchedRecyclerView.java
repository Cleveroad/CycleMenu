package com.cleveroad.sy.cyclemenuwidget;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/**
 * TouchedRecyclerView are used as RecycleView that allow to control touch interception.
 */
class TouchedRecyclerView extends RecyclerView {

    private boolean mTouchEnabled = true;
    private boolean mHasItemsToScroll = true;
    private float lastX;
    private float lastY;
    private boolean mIsScrolling;

    public TouchedRecyclerView(Context context) {
        super(context);
    }

    public TouchedRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TouchedRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setTouchEnabled(boolean touchEnabled) {
        mTouchEnabled = touchEnabled;
    }

    public void setHasItemsToScroll(boolean hasItemsToScroll) {
        mHasItemsToScroll = hasItemsToScroll;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        if (mTouchEnabled) {
            return super.onInterceptTouchEvent(ev);
        }

        if (!mHasItemsToScroll) {
            return mTouchEnabled;
        }
        //if we haven't enough elements to scroll need to intercept touch but do not intercepot scroll.

        ViewConfiguration vc = ViewConfiguration.get(getContext());
        int slop = vc.getScaledTouchSlop();
        int action = MotionEventCompat.getActionMasked(ev);

        // Always handle the case of the touch gesture being complete.
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            // Release the scroll.
            mIsScrolling = false;
            return false; // Do not intercept touch event, let the child handle it
        }

        // If the user has dragged her finger horizontally more than
        // the touch slop, start the scroll
        // left as an exercise for the reader
        // Touch slop should be calculated using ViewConfiguration
        // constants.
        if (action == MotionEvent.ACTION_MOVE) {
            if (mIsScrolling) {
                // We're currently scrolling, so yes, intercept the
                // touch event!
                return true;
            }
            double diff = Math.sqrt((ev.getX() - lastX) * (ev.getX() - lastX) + (ev.getY() - lastY) * (ev.getY() - lastY));
            lastX = ev.getX();
            lastY = ev.getY();
            if (diff > slop) {
                // Start scrolling!
                mIsScrolling = true;
                return true;
            }
        }

        // In general, we don't want to intercept touch events. They should be
        // handled by the child view.
        return false;

    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {

        if (mTouchEnabled && mHasItemsToScroll) {
            getParent().requestDisallowInterceptTouchEvent(true);
            return super.onTouchEvent(e);
        }
        return mTouchEnabled;
    }
}
