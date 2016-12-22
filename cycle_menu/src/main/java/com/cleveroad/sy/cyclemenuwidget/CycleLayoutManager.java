package com.cleveroad.sy.cyclemenuwidget;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;

import com.cleveroad.sy.cyclemenuwidget.CycleMenuWidget.CORNER;

public class CycleLayoutManager extends RecyclerView.LayoutManager {

    /**
     * Scaling coefficient that is used for increasing spaces between two items in lollipop
     */
    private static final double SCALING_COEFFICIENT = 1.3;

    /**
     * Half of the margin from item side. Is used to calculate item scroll possibility.
     */
    private int mHalfAdditionalMargin = 0;

    /**
     * Can disable/enable scrolling. Can be set via setter. Is used in @CycleMenuWidget class
     */
    private boolean mScrollEnabled = false;

    /**
     * Specifies corner which will be set to layout menu cycle
     */
    private CORNER mCurrentCorner = CORNER.RIGHT_TOP;

    /**
     * Used to prevent scrolling when measureChildWithMargins called
     */
    private boolean mCanScroll = true;

    /**
     * Calculated margin of each item. Used to calculate animation shift in rollInAnimation.
     */
    private double mMarginAngle;

    /**
     * Additional margin for items in preLollipop device.
     * In preLollipop device FloatingActionButton has additional margins from the sides.
     */
    private float mPreLollipopAdditionalButtonsMargin = 0;

    /**
     * View cache that is used to recycler and remove all not used views in fill method.
     */
    private SparseArray<View> mViewCache = new SparseArray<>();
    /**
     * Angles of each item view. Used in internalScroll method.
     */
    private SparseArray<Double> mViewAngles;
    /**
     * Angle that view item has per own diameter.
     */
    private double mAnglePerItem = -1;
    /**
     * Calculated radius of the cycle menu.
     */
    private int mRadius = 10;
    /**
     * Used to indicate if are there available amount of items for scrolling.
     */
    private Boolean mScrollIsAvailableDueToChildrenCount;
    /**
     * Predefined position of the first element
     */
    private int mScrollToPosition = RecyclerView.NO_POSITION;
    /**
     * Predefined angle shift in degrees of the first element.
     */
    private double mAdditionalAngleOffset = CycleMenuWidget.UNDEFINED_ANGLE_VALUE;

    public CycleLayoutManager(Context context, CORNER corner ) {
        mCurrentCorner = corner;
        mPreLollipopAdditionalButtonsMargin = context.getResources().getDimensionPixelSize(R.dimen.cm_prelollipop_additional_margin);
        mViewAngles = new SparseArray<>();
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.MATCH_PARENT);
    }

    private int getRadius() {
        return mRadius;
    }

    private void setRadius(int radius) {
        mRadius = radius;
    }

    void setCorner(@NonNull CORNER currentCorner) {
        mCurrentCorner = currentCorner;
    }

    void setScrollEnabled(boolean scrollEnabled) {
        mScrollEnabled = scrollEnabled;
    }

    @Override
    public boolean canScrollVertically() {
        return mCanScroll && mScrollEnabled && (mScrollIsAvailableDueToChildrenCount == null || mScrollIsAvailableDueToChildrenCount);
    }

    @Override
    public boolean canScrollHorizontally() {
        return mCanScroll && mScrollEnabled && (mScrollIsAvailableDueToChildrenCount == null || mScrollIsAvailableDueToChildrenCount);
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        mScrollToPosition = RecyclerView.NO_POSITION;
        if (mScrollEnabled) {
            return internalScrollBy((mCurrentCorner == CORNER.RIGHT_TOP || mCurrentCorner == CORNER.LEFT_BOTTOM) ? dx : -dx, recycler);
        }
        return 0;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        mScrollToPosition = RecyclerView.NO_POSITION;
        if (mScrollEnabled) {
            return internalScrollBy(dy, recycler);
        }
        return 0;
    }

    private int internalScrollBy(int dScroll, RecyclerView.Recycler recycler) {
        int childCount = getChildCount();
        if (childCount == 0) {
            return 0;
        }
        mScrollIsAvailableDueToChildrenCount = true;
        int delta;
        //need to use upToDown calculation if the menu has bottom orientation.
        if (mCurrentCorner.isBottomSide()) {
            delta = checkEndsReached(-dScroll);
        } else {
            delta = checkEndsReached(dScroll);
        }

        int radius = getRadius();
        //Length of the circle of the menu.
        double circleLength = 2 * Math.PI * getRadius();
        //Approximately calculated angle that menu need to be scrolled on
        double angleToRotate = 360.0 * delta / circleLength;

        for (int indexOfView = 0; indexOfView < childCount; indexOfView++) {
            View view = getChildAt(indexOfView);
            int viewPosition = getPosition(view);
            //Save new angle of the view item
            mViewAngles.put(viewPosition, angleToRotate + mViewAngles.get(viewPosition));

            //current position of the view item
            double viewCenterX = view.getRight() - view.getWidth() / 2.0;
            double viewCenterY = view.getTop() + view.getHeight() / 2.0;

            //new position for the view item
            double newCenterX = radius * Math.cos(mViewAngles.get(viewPosition) * Math.PI / 180);
            double newCenterY = radius * Math.sin(mViewAngles.get(viewPosition) * Math.PI / 180);

            if (mCurrentCorner == CORNER.RIGHT_TOP) {
                newCenterX = getWidth() - newCenterX;
            } else if (mCurrentCorner == CORNER.LEFT_BOTTOM) {
                newCenterY = getHeight() - newCenterY;
            } else if (mCurrentCorner == CORNER.RIGHT_BOTTOM) {
                newCenterX = getWidth() - newCenterX;
                newCenterY = getHeight() - newCenterY;
            }

            int dx = (int) Math.round(newCenterX - viewCenterX);
            int dy = (int) Math.round(newCenterY - viewCenterY);

            view.offsetTopAndBottom(dy);
            view.offsetLeftAndRight(dx);
        }
        //refill items after scroll
        fill(recycler);
        //need to use upToDown calculation if the menu has bottom orientation.
        if (mCurrentCorner.isBottomSide()) {
            return delta;
        }
        return -delta;
    }

    /**
     * Method to check if the end is reached with scrolling
     *
     * @param dy value to scroll
     * @return available value to scroll.
     */
    private int checkEndsReached(int dy) {
        int childCount = getChildCount();
        int itemCount = getItemCount();
        if (childCount == 0) {
            return 0;
        }

        int delta = 0;
        View firstChildView = getChildAt(0);
        View lastChildView = getChildAt(childCount - 1);

        if (dy < 0) { //scroll to bottom if menu corner is top side, to up if menu corner is bottom side
            if (getPosition(lastChildView) < itemCount - 1) { //if last item not reached
                delta = dy;
            } else { //if last item reached
                if (mCurrentCorner.isBottomSide()) { //scroll from bottom to up
                    int viewBottom = getDecoratedBottom(lastChildView);
                    int parentBottom = getHeight();
                    delta = Math.max(parentBottom - mHalfAdditionalMargin - viewBottom, dy);
                } else { //scroll from up to down
                    int viewTop = getDecoratedTop(lastChildView);
                    delta = Math.max(viewTop - mHalfAdditionalMargin, dy);
                }
            }
        } else if (dy > 0) { //scroll to up if menu corner is top side, to bottom if menu corner is bottom side
            if (getPosition(firstChildView) > 0) { //if first item not reached
                delta = dy;
            } else {
                //if first item reached
                if (mCurrentCorner.isLeftSide()) {
                    int viewLeft = getDecoratedLeft(firstChildView);
                    int parentLeft = 0;
                    delta = Math.min(parentLeft - viewLeft + mHalfAdditionalMargin, dy);
                } else {
                    int viewRight = getDecoratedRight(firstChildView);
                    int parentRight = getWidth();
                    delta = Math.min(viewRight + mHalfAdditionalMargin - parentRight, dy);
                }
            }
        }
        return -delta;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        mAnglePerItem = -1;
        detachAndScrapAttachedViews(recycler);
        if (getWidth() > 0 && getHeight() > 0 && getWidth() < 10000 && getHeight() < 10000) {
            fill(recycler);
        }
    }

    public void fill(RecyclerView.Recycler recycler) {
        View anchorView = getAnchorView();
        mViewCache.clear();

        for (int i = 0, cnt = getChildCount(); i < cnt; i++) {
            View view = getChildAt(i);
            int pos = getPosition(view);
            mViewCache.put(pos, view);
        }

        for (int i = 0; i < mViewCache.size(); i++) {
            detachView(mViewCache.valueAt(i));
        }
        fillUp(anchorView, recycler);
        fillDown(anchorView, recycler);

        for (int i = 0; i < mViewCache.size(); i++) {
            recycler.recycleView(mViewCache.valueAt(i));
        }
    }

    /**
     * fill to up items from the anchor item
     *
     * @param anchorView
     * @param recycler
     */
    private void fillUp(@Nullable View anchorView, RecyclerView.Recycler recycler) {
        int anchorPos;
        if (anchorView != null) {
            anchorPos = getPosition(anchorView);
        } else {
            return;
        }

        int pos = anchorPos - 1;
        if (mScrollToPosition != RecyclerView.NO_POSITION) {
            pos = mScrollToPosition - 1;
        }
        boolean canFillUp;
        int radius = getRadius();
        double angle;
        if (mCurrentCorner.isLeftSide()) {
            canFillUp = anchorView.getLeft() > 0;
        } else {
            canFillUp = anchorView.getRight() < getWidth();
        }
        angle = mViewAngles.get(anchorPos) + mAnglePerItem;
        //Can be used View.MeasureSpec.AT_MOST because items is floating action buttons
        final int widthSpec = View.MeasureSpec.makeMeasureSpec(getWidth(), View.MeasureSpec.AT_MOST);
        final int heightSpec = View.MeasureSpec.makeMeasureSpec(getHeight(), View.MeasureSpec.AT_MOST);

        int top;
        int bottom;
        int right;
        int left;

        while (canFillUp && pos >= 0) {
            View view = mViewCache.get(pos);

            if (view == null) {
                mViewAngles.put(pos, angle);
                view = recycler.getViewForPosition(pos);
                addView(view, 0);
                measureChildWithMargins(view, widthSpec, heightSpec);
                int decoratedMeasuredWidth = getDecoratedMeasuredWidth(view);
                int decoratedMeasuredHeight = getDecoratedMeasuredHeight(view);

                //position of the new item
                int xDistance = (int) (radius * Math.cos(angle * Math.PI / 180));
                int yDistance = (int) (radius * Math.sin(angle * Math.PI / 180));

                left = xDistance - decoratedMeasuredWidth / 2;
                right = xDistance + decoratedMeasuredWidth / 2;
                top = yDistance - decoratedMeasuredHeight / 2;
                bottom = yDistance + decoratedMeasuredHeight / 2;

                //changes for each corners except left_top
                if (mCurrentCorner == CORNER.RIGHT_TOP) {
                    left = getWidth() - xDistance - decoratedMeasuredWidth / 2;
                    right = getWidth() - xDistance + decoratedMeasuredWidth / 2;
                } else if (mCurrentCorner == CORNER.LEFT_BOTTOM) {
                    top = getHeight() - yDistance - decoratedMeasuredHeight / 2;
                    bottom = getHeight() - yDistance + decoratedMeasuredHeight / 2;
                } else if (mCurrentCorner == CORNER.RIGHT_BOTTOM) {
                    left = getWidth() - xDistance - decoratedMeasuredWidth / 2;
                    right = getWidth() - xDistance + decoratedMeasuredWidth / 2;
                    top = getHeight() - yDistance - decoratedMeasuredHeight / 2;
                    bottom = getHeight() - yDistance + decoratedMeasuredHeight / 2;
                }

                layoutDecorated(view, left, top, right, bottom);

            } else {
                attachView(view);
                mViewCache.remove(pos);
                left = view.getLeft();
                right = view.getRight();
            }
            pos--;
            //Check if top not reached
            if (mCurrentCorner.isLeftSide()) {
                canFillUp = left > 0;
            } else if (mCurrentCorner.isRightSide()) {
                canFillUp = right < getWidth();
            }
            angle += mAnglePerItem;

        }
    }

    /**
     * fill to up down from the anchor item
     *
     * @param anchorView
     * @param recycler
     */
    private void fillDown(@Nullable View anchorView, RecyclerView.Recycler recycler) {

        int anchorPos = 0;
        if (anchorView != null) {
            anchorPos = getPosition(anchorView);
        }
        int pos = anchorPos;
        if (mScrollToPosition != RecyclerView.NO_POSITION) {
            pos = mScrollToPosition;
        }
        boolean canFillDown = true;
        int itemCount = getItemCount();
        //Can be used View.MeasureSpec.AT_MOST because items is floating action buttons
        final int widthSpec = View.MeasureSpec.makeMeasureSpec(getWidth(), View.MeasureSpec.AT_MOST);
        final int heightSpec = View.MeasureSpec.makeMeasureSpec(getHeight(), View.MeasureSpec.AT_MOST);

        double angle = 90;
        if (anchorView != null) {
            angle = mViewAngles.get(pos);
        }
        int left;
        int top;
        int right;
        int bottom;
        while (canFillDown && pos < itemCount) {
            View view = mViewCache.get(pos);
            if (view == null) {
                view = recycler.getViewForPosition(pos);
                addView(view);
                measureChildWithMargins(view, widthSpec, heightSpec);
                int decoratedMeasuredWidth = getDecoratedMeasuredWidth(view);
                int decoratedMeasuredHeight = getDecoratedMeasuredHeight(view);
                if (mAnglePerItem < 0) { //if not initialized
                    //calculate and set radius of the menu
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        setRadius((getWidth() > getHeight() ? getHeight() : getWidth()) - decoratedMeasuredHeight * 4 / 5);
                    } else {
                        setRadius((getWidth() > getHeight() ? getHeight() : getWidth()) - decoratedMeasuredHeight / 2);
                    }
                    //Calculate margins between the items.
                    double circleLength = 2 * Math.PI * getRadius();
                    double anglePerLength;
                    double anglePerLengthWithMargins;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        anglePerLength = 360.0 * (decoratedMeasuredHeight) / circleLength;
                        anglePerLengthWithMargins = anglePerLength * SCALING_COEFFICIENT;
                        mMarginAngle = (anglePerLengthWithMargins - anglePerLength) / 2.0;
                        mHalfAdditionalMargin = (int) ((decoratedMeasuredHeight * SCALING_COEFFICIENT - decoratedMeasuredHeight) / 2.0);
                    } else {
                        //In preLollipop android floatingActionButton has additional margin.
                        anglePerLength = 360.0 * (decoratedMeasuredHeight - mPreLollipopAdditionalButtonsMargin * 2) / circleLength;
                        anglePerLengthWithMargins = 360.0 * (decoratedMeasuredHeight - mPreLollipopAdditionalButtonsMargin * 2 / 1.5f) / circleLength;
                        mMarginAngle = (anglePerLengthWithMargins - anglePerLength) / 2.0;
                    }
                    if (mAdditionalAngleOffset < -999) {
                        angle -= anglePerLengthWithMargins / 2.0;
                    } else {
                        angle -= mAdditionalAngleOffset;
                    }
                    mAnglePerItem = anglePerLengthWithMargins;
                }
                mViewAngles.put(pos, angle);
                int xDistance = (int) (getRadius() * Math.cos(angle * Math.PI / 180));
                int yDistance = (int) (getRadius() * Math.sin(angle * Math.PI / 180));

                left = xDistance - decoratedMeasuredWidth / 2;
                right = xDistance + decoratedMeasuredWidth / 2;
                top = yDistance - decoratedMeasuredHeight / 2;
                bottom = yDistance + decoratedMeasuredHeight / 2;
                if (mCurrentCorner == CORNER.RIGHT_TOP) {
                    left = getWidth() - xDistance - decoratedMeasuredWidth / 2;
                    right = getWidth() - xDistance + decoratedMeasuredWidth / 2;
                } else if (mCurrentCorner == CORNER.LEFT_BOTTOM) {
                    top = getHeight() - yDistance - decoratedMeasuredHeight / 2;
                    bottom = getHeight() - yDistance + decoratedMeasuredHeight / 2;
                } else if (mCurrentCorner == CORNER.RIGHT_BOTTOM) {
                    left = getWidth() - xDistance - decoratedMeasuredWidth / 2;
                    right = getWidth() - xDistance + decoratedMeasuredWidth / 2;
                    top = getHeight() - yDistance - decoratedMeasuredHeight / 2;
                    bottom = getHeight() - yDistance + decoratedMeasuredHeight / 2;
                }

                layoutDecorated(view, left, top, right, bottom);
            } else {

                attachView(view);
                mViewCache.remove(pos);
                top = view.getTop();
                bottom = view.getBottom();
            }

            if (mCurrentCorner.isUpSide()) {
                canFillDown = top > 0;
            } else {
                canFillDown = bottom < getHeight();
            }
            pos++;
            if (pos == itemCount && mScrollIsAvailableDueToChildrenCount == null) {
                mScrollIsAvailableDueToChildrenCount = !canFillDown;
            }
            angle -= mAnglePerItem;
        }
    }

    /**
     * Getting anchor view for the filling.
     * The first partially visible item
     */
    private View getAnchorView() {
        View anchorView;
        int childCount = getChildCount();
        if (childCount == 0) {
            return null;
        }
        int anchorViewPosition = 0;
        if (mCurrentCorner.isLeftSide()) {
            do {
                anchorView = getChildAt(anchorViewPosition);
                anchorViewPosition++;
            } while (anchorView.getRight() < 0 && anchorViewPosition < childCount);
        } else {
            do {
                anchorView = getChildAt(anchorViewPosition);
                anchorViewPosition++;
            } while (anchorView.getLeft() > getWidth() && anchorViewPosition < childCount);
        }

        return anchorView;
    }

    @Override
    public void measureChildWithMargins(View child, int widthSpec, int heightSpec) {
        // change a value to "false "temporary while measuring
        mCanScroll = false;

        Rect decorRect = new Rect();
        calculateItemDecorationsForChild(child, decorRect);
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();

        int lWidthSpec = updateSpecWithExtra(widthSpec, lp.leftMargin + decorRect.left, lp.rightMargin + decorRect.right);
        int lHeightSpec = updateSpecWithExtra(heightSpec, lp.topMargin + decorRect.top, lp.bottomMargin + decorRect.bottom);
        child.measure(lWidthSpec, lHeightSpec);

        // return a value to "true" because we do actually can scroll in both ways
        mCanScroll = true;
    }

    private int updateSpecWithExtra(int spec, int startInset, int endInset) {
        if (startInset == 0 && endInset == 0) {
            return spec;
        }
        final int mode = View.MeasureSpec.getMode(spec);
        if (mode == View.MeasureSpec.AT_MOST || mode == View.MeasureSpec.EXACTLY) {
            return View.MeasureSpec.makeMeasureSpec(
                    View.MeasureSpec.getSize(spec) - startInset - endInset, mode);
        }
        return spec;
    }

    /**
     * Get current position of the first item from adapter.
     */
    int getCurrentPosition() {
        if (getChildCount() > 0) {
            return getPosition(getChildAt(0));
        }
        return RecyclerView.NO_POSITION;
    }

    /**
     * Getting shift angle of the items in degree.
     */
    double getCurrentItemsAngleOffset() {
        if (getChildCount() > 0) {
            return 90 - mViewAngles.get(getPosition(getChildAt(0)));
        }
        return 0;
    }

    /**
     * Set shift angle of the items in degree.
     */
    void setAdditionalAngleOffset(double additionalAngleOffset) {
        mAdditionalAngleOffset = additionalAngleOffset;
    }

    /**
     * @return true if count pof item availabkle to scroll
     */
    public boolean isCountOfItemsAvailableToScroll() {
        return mScrollIsAvailableDueToChildrenCount == null || mScrollIsAvailableDueToChildrenCount;
    }


    void rollInItemsWithAnimation(final OnCompleteCallback callback) {
        final int childCount = getChildCount();
        if (childCount == 0) {
            callback.onComplete();
            return;
        }
        int overshootCoefficient = 6;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            overshootCoefficient = 6;
        }
        int duration = 300;

        int startOffset = duration / childCount;

        for (int i = 0; i < childCount; i++) {
            final View view = getChildAt(i);
            float animationRotateAnchorX = 0;
            float animationRotateAnchorY = 0;
            float startDegree = 100;
            float overshootDegree = (float) ((i + overshootCoefficient) * mMarginAngle * 2f);
            if (mCurrentCorner == CORNER.LEFT_TOP) {
                startDegree = -100;
                animationRotateAnchorX = -view.getLeft();
                animationRotateAnchorY = -view.getTop();
            } else if (mCurrentCorner == CORNER.RIGHT_TOP) {
                startDegree = 100;
                overshootDegree = -overshootDegree;
                animationRotateAnchorX = getWidth() - view.getLeft();
                animationRotateAnchorY = -view.getTop();
            } else if (mCurrentCorner == CORNER.LEFT_BOTTOM) {
                startDegree = 100;
                overshootDegree = -overshootDegree;
                animationRotateAnchorX = -view.getLeft();
                animationRotateAnchorY = getHeight() - view.getTop();
            } else if (mCurrentCorner == CORNER.RIGHT_BOTTOM) {
                startDegree = -100;
                animationRotateAnchorX = getWidth() - view.getLeft();
                animationRotateAnchorY = getHeight() - view.getTop();
            }

            RotateAnimation rotateAnimation = new RotateAnimation(startDegree, overshootDegree, Animation.ABSOLUTE, animationRotateAnchorX, Animation.ABSOLUTE, animationRotateAnchorY);
            rotateAnimation.setDuration(duration);
            rotateAnimation.setStartOffset(startOffset * i / 2);
            rotateAnimation.setFillBefore(true);
            rotateAnimation.setInterpolator(new DecelerateInterpolator());

            final RotateAnimation backRotateAnimation = new RotateAnimation(overshootDegree, 0, Animation.ABSOLUTE, animationRotateAnchorX, Animation.ABSOLUTE, animationRotateAnchorY);
            backRotateAnimation.setDuration((i + overshootCoefficient) * startOffset / 2);
            backRotateAnimation.setFillBefore(true);
            backRotateAnimation.setInterpolator(new LinearInterpolator());

            backRotateAnimation.setStartOffset((getChildCount() - i - 1) * startOffset / 2);
            final int finalI = i;
            rotateAnimation.setAnimationListener(new AnimationListenerAdapter() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    if (finalI == childCount - 1) {
                        backRotateAnimation.setAnimationListener(new AnimationListenerAdapter() {

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                callback.onComplete();
                            }
                        });
                    }
                    view.startAnimation(backRotateAnimation);
                }
            });
            view.startAnimation(rotateAnimation);
        }
    }

    void rollOutItemsWithAnimation(final OnCompleteCallback callback) {
        if (getChildCount() == 0) {
            callback.onComplete();
            return;
        }
        int duration = 300;

        int startOffset = 50;
        for (int i = getChildCount() - 1; i >= 0; i--) {

            final View view = getChildAt(i);
            view.clearAnimation();
            float animationRotateAnchorX = 0;
            float animationRotateAnchorY = 0;
            float overshootDegree = 0;
            if (mCurrentCorner == CORNER.LEFT_TOP) {
                overshootDegree = -100;
                animationRotateAnchorX = -view.getLeft();
                animationRotateAnchorY = -view.getTop();
            } else if (mCurrentCorner == CORNER.RIGHT_TOP) {
                overshootDegree = 100;
                animationRotateAnchorX = getWidth() - view.getLeft();
                animationRotateAnchorY = -view.getTop();
            } else if (mCurrentCorner == CORNER.LEFT_BOTTOM) {
                overshootDegree = 100;
                animationRotateAnchorX = -view.getLeft();
                animationRotateAnchorY = getHeight() - view.getTop();
            } else if (mCurrentCorner == CORNER.RIGHT_BOTTOM) {
                overshootDegree = -100;
                animationRotateAnchorX = getWidth() - view.getLeft();
                animationRotateAnchorY = getHeight() - view.getTop();
            }

            RotateAnimation rotateAnimation = new RotateAnimation(0, overshootDegree, Animation.ABSOLUTE, animationRotateAnchorX, Animation.ABSOLUTE, animationRotateAnchorY);
            rotateAnimation.setDuration(duration);
            rotateAnimation.setStartOffset(startOffset * (getChildCount() - i - 1));
            rotateAnimation.setFillAfter(true);
            rotateAnimation.setInterpolator(new DecelerateInterpolator());

            if (i == 0) {
                rotateAnimation.setAnimationListener(new AnimationListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        callback.onComplete();
                    }
                });
            }
            view.startAnimation(rotateAnimation);
        }

    }

    @Override
    public void scrollToPosition(int position) {
        mScrollToPosition = position;
        requestLayout();
    }

    interface OnCompleteCallback {

        void onComplete();

    }
}
