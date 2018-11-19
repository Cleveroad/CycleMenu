package com.cleveroad.sy.cyclemenuwidget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import java.util.Collection;

public class CycleMenuWidget extends ViewGroup {

    public static final int UNDEFINED_ANGLE_VALUE = -1000;
    private static final int CENTER_IMAGE_ROTATE_DURATION = 300;
    private static final int REVEAL_ANIMATION_DURATION = 200;
    private static final int RIPPLE_REVEAL_DURATION = 300;
    private static final int RIPPLE_ALPHA_DURATION = 450;

    private static final String RIPPLE_RADIUS_ANIMATOR_FIELD_NAME = "rippleRadius";
    private static final String RIPPLE_ALPHA_ANIMATOR_FIELD_NAME = "rippleAlpha";
    private static final String CIRCLE_RADIUS_ANIMATOR_FIELD_NAME = "animationCircleRadius";
    private static final String SHADOW_SIZE_ANIMATOR_FIELD_NAME = "variableShadowSize";

    private static final String FIELD_NAME_FOR_EXCEPTION_ITEM = "item";
    private static final String FIELD_NAME_FOR_EXCEPTION_MENU = "menu";
    private static final String FIELD_NAME_FOR_EXCEPTION_ITEMS = "items";
    private static final String FIELD_NAME_FOR_EXCEPTION_CORNER = "corner";
    private static final String FIELD_NAME_FOR_EXCEPTION_SCALING_TYPE = "scalingType";
    private static final String FIELD_NAME_FOR_EXCEPTION_SCROLLING_TYPE = "scrollingType";

    private static final int DEFAULT_UNDEFINED_VALUE = -1;
    private static final float SHADOW_SIZE_MIN_COEFFICIENT = 0.25f;

    /**
     * Specifies states of cycle menu widget. If mState is IN_OPEN_PROCESS or IN_CLOSE_PROCESS then clicks will not be handled.
     */
    public enum STATE {
        OPEN, CLOSED, IN_OPEN_PROCESS, IN_CLOSE_PROCESS
    }

    private STATE mState = STATE.CLOSED;

    /**
     * Specifies corner which will be set to layout menu cycle
     */
    public enum CORNER {
        LEFT_TOP(INNER_CORNER.LEFT_TOP),
        RIGHT_TOP(INNER_CORNER.RIGHT_TOP),
        LEFT_BOTTOM(INNER_CORNER.LEFT_BOTTOM),
        RIGHT_BOTTOM(INNER_CORNER.RIGHT_BOTTOM);

        /**
         * Specified for checking for side inside the parent enum
         */
        enum INNER_CORNER {
            LEFT_TOP(0), RIGHT_TOP(1), LEFT_BOTTOM(2), RIGHT_BOTTOM(3);

            private final int mValue;

            INNER_CORNER(int value) {
                mValue = value;
            }

            public int getValue() {
                return mValue;
            }

        }

        private final INNER_CORNER mInnerCorner;

        CORNER(INNER_CORNER innerCorner) {
            mInnerCorner = innerCorner;
        }

        public boolean isLeftSide() {
            return mInnerCorner == INNER_CORNER.LEFT_TOP || mInnerCorner == INNER_CORNER.LEFT_BOTTOM;
        }

        public boolean isUpSide() {
            return mInnerCorner == INNER_CORNER.LEFT_TOP || mInnerCorner == INNER_CORNER.RIGHT_TOP;
        }

        public boolean isBottomSide() {
            return mInnerCorner == INNER_CORNER.LEFT_BOTTOM || mInnerCorner == INNER_CORNER.RIGHT_BOTTOM;
        }

        public boolean isRightSide() {
            return mInnerCorner == INNER_CORNER.RIGHT_TOP || mInnerCorner == INNER_CORNER.RIGHT_BOTTOM;
        }

        public int getValue() {
            return mInnerCorner.getValue();
        }

        public static CORNER valueOf(int value) {
            switch (value) {
                case 0:
                    return LEFT_TOP;
                case 2:
                    return LEFT_BOTTOM;
                case 3:
                    return RIGHT_BOTTOM;
                case 1:
                default:
                    return RIGHT_TOP;
            }
        }
    }

    private CORNER mCorner = CORNER.RIGHT_TOP;

    /**
     * Specified radius scaling type.
     * If AUTO then size can be increased to the max_auto_radius_size if there is a lot of items in menu
     * or decreased to the min_auto_radius_size if the count of items is little.
     * If FIXED then radius will be set exactly to the value specified in fixed_radius.
     * If fixed radius is bigger the available size of widget it will be decreased.
     */
    public enum RADIUS_SCALING_TYPE {
        AUTO(0),
        FIXED(1);

        private final int mValue;

        RADIUS_SCALING_TYPE(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static RADIUS_SCALING_TYPE valueOf(int value) {
            if (value == 0) {
                return AUTO;
            }
            return FIXED;
        }
    }

    private RADIUS_SCALING_TYPE mScalingType = RADIUS_SCALING_TYPE.AUTO;

    /**
     * The scroll type specified if the scroll will be infinite (ENDLESS) or will have bounds (BASIC).
     * If scroll type is set to ENDLESS but there is no elements to scroll, the scrolling type will be changed to BASIC.
     */
    public enum SCROLL {
        BASIC(0),
        ENDLESS(1);

        private final int mValue;

        SCROLL(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static SCROLL valueOf(int value) {
            if (value == 0) {
                return BASIC;
            }
            return ENDLESS;
        }
    }

    private SCROLL mScrollType = SCROLL.ENDLESS;

    private OnStateChangedListener mOnStateChangeListener;
    private StateSaveListener mStateSaveListener;

    private boolean mShouldOpen = false;
    private float mShadowSize = 40;
    private float mPreLollipopAdditionalButtonsMargin = 0;
    private float mVariableShadowSize = 45;
    private int mOutCircleRadius = 0;

    /**
     * Colors for the shadow gradient
     */
    private int mShadowStartColor;
    private int mShadowMiddleColor;
    private int mShadowEndColor;

    /**
     * CycleMenuWidget background color
     */
    private int mBackgroundColor;

    /**
     * Paint for background circle
     */
    private Paint mCirclePaint;
    /**
     * Paint ripple circles that appears when user touch central image button (in the corner).
     */
    private Paint mRipplePaint;
    /**
     * Color for the ripple effect of touching central image button (in the corner)
     */
    private int mRippleColor = DEFAULT_UNDEFINED_VALUE;
    /**
     * Radius for drawing ripple effect
     */
    private int mRippleRadius;

    /**
     * Paint shadow around background circle
     */
    private Paint mCornerShadowPaint;
    /**
     * Path for the shadow around the background circle
     */
    private Path mCornerShadowPath;

    /**
     * Minimal circle radius for the background
     */
    private int mCircleMinRadius = DEFAULT_UNDEFINED_VALUE;
    /**
     * Radius for drawing background circle
     */
    private int mAnimationCircleRadius = 200;

    /**
     * Position from the adapter of the current first item.
     */
    private int mCurrentPosition = RecyclerView.NO_POSITION;

    /**
     * Angle in degrees from the layout manager to be saved if holder with cycleMenuWidget will be reused.
     */
    private double mCurrentAngleOffset = UNDEFINED_ANGLE_VALUE;

    /**
     * Size of the one item element
     */
    private int mItemSize = -1;
    /**
     * Size of inner menu recycler view
     */
    private int mRecyclerSize = -1;

    /**
     * Image for the image button placed in the corner.
     */
    private ImageView mCenterImage;
    /**
     * Recycler view which contains items of the menu
     */
    private TouchedRecyclerView mRecyclerView;
    /**
     * Layout manager that place items in the circular way
     */
    private CycleLayoutManager mLayoutManager;

    /**
     * Background tint for the items
     */
    private ColorStateList mItemsBackgroundTint;

    /**
     * Radiuses to be used to calculate real radius of the items recyclerView
     */
    private int mAutoMinRadius = 0;
    private int mAutoMaxRadius = 0;
    private int mFixedRadius = 0;

    /**
     * State of the layout manager and recyclerMenuAdapter
     */
    private boolean mInitialized = false;

    /**
     * Adapter for recycler view which will be produce items for the circular menu
     */
    private RecyclerMenuAdapter mAdapter;

    /**
     * When widget is used in the recyclerView item. It need to be requested to relayout itself.
     * runnableRequestLayout is used for that reason
     */
    private Runnable runnableRequestLayout = new Runnable() {
        @Override
        public void run() {
            mLayoutManager.requestLayout();
        }
    };

    public CycleMenuWidget(Context context) {
        this(context, null);
    }

    public CycleMenuWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CycleMenuWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    @SuppressWarnings("unused")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CycleMenuWidget(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setWillNotDraw(false);
        TypedArray typedArrayValues = context.obtainStyledAttributes(attrs, R.styleable.CycleMenuWidget);
        mItemsBackgroundTint = typedArrayValues.getColorStateList(R.styleable.CycleMenuWidget_cm_item_background_tint);
        mCorner = CORNER.valueOf(typedArrayValues.getInt(R.styleable.CycleMenuWidget_cm_corner, CORNER.RIGHT_TOP.getValue()));
        mAutoMinRadius = typedArrayValues.getDimensionPixelSize(R.styleable.CycleMenuWidget_cm_autoMinRadius, DEFAULT_UNDEFINED_VALUE);
        mAutoMaxRadius = typedArrayValues.getDimensionPixelSize(R.styleable.CycleMenuWidget_cm_autoMaxRadius, DEFAULT_UNDEFINED_VALUE);
        mFixedRadius = typedArrayValues.getDimensionPixelSize(R.styleable.CycleMenuWidget_cm_fixedRadius, DEFAULT_UNDEFINED_VALUE);
        mScalingType = RADIUS_SCALING_TYPE.valueOf(typedArrayValues.getInt(R.styleable.CycleMenuWidget_cm_radius_scale_type, RADIUS_SCALING_TYPE.AUTO.getValue()));
        mScrollType = SCROLL.valueOf(typedArrayValues.getInt(R.styleable.CycleMenuWidget_cm_scroll_type, SCROLL.BASIC.getValue()));
        Drawable cornerImageDrawable = typedArrayValues.getDrawable(R.styleable.CycleMenuWidget_cm_corner_image_src);
        mRippleColor = typedArrayValues.getColor(R.styleable.CycleMenuWidget_cm_ripple_color, DEFAULT_UNDEFINED_VALUE);
        setCollapsedRadius(typedArrayValues.getDimensionPixelSize(R.styleable.CycleMenuWidget_cm_collapsed_radius, DEFAULT_UNDEFINED_VALUE));
        mBackgroundColor = typedArrayValues.getColor(R.styleable.CycleMenuWidget_cm_background, DEFAULT_UNDEFINED_VALUE);
        typedArrayValues.recycle();

        mCirclePaint = new Paint();
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setColor(Color.WHITE);
        mCirclePaint.setStyle(Paint.Style.FILL);

        if (mRippleColor == DEFAULT_UNDEFINED_VALUE) {
            mRippleColor = ContextCompat.getColor(getContext(), R.color.cm_ripple_color);
        }
        mRipplePaint = new Paint();
        mRipplePaint.setAntiAlias(true);
        mRipplePaint.setStyle(Paint.Style.FILL);
        mRipplePaint.setColor(mRippleColor);

        mCornerShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mCornerShadowPaint.setStyle(Paint.Style.FILL);

        mShadowStartColor = ContextCompat.getColor(getContext(), R.color.cm_shadow_start_color);
        mShadowMiddleColor = ContextCompat.getColor(getContext(), R.color.cm_shadow_mid_color);
        mShadowEndColor = ContextCompat.getColor(getContext(), R.color.cm_shadow_end_color);

        mShadowSize = getResources().getDimensionPixelSize(R.dimen.cm_main_shadow_size);
        mVariableShadowSize = mShadowSize * SHADOW_SIZE_MIN_COEFFICIENT;
        mPreLollipopAdditionalButtonsMargin = getContext().getResources().getDimensionPixelSize(R.dimen.cm_prelollipop_additional_margin);
        if (mCircleMinRadius == DEFAULT_UNDEFINED_VALUE) {
            mCircleMinRadius = getContext().getResources().getDimensionPixelSize(R.dimen.cm_circle_min_radius);
        }
        mAnimationCircleRadius = mCircleMinRadius;

        mRecyclerView = new TouchedRecyclerView(getContext());
        mRecyclerView.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
        mLayoutManager = new CycleLayoutManager(getContext(), mCorner);
        addView(mRecyclerView);

        mAdapter = new RecyclerMenuAdapter();
        if (mItemsBackgroundTint != null) {
            mAdapter.setItemsBackgroundTint(mItemsBackgroundTint);
        }
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
        mCenterImage = new ImageView(getContext());
        if (cornerImageDrawable != null) {
            mCenterImage.setImageDrawable(cornerImageDrawable);
        } else {
            mCenterImage.setImageResource(R.drawable.cm_ic_plus);
        }
        if (mBackgroundColor != DEFAULT_UNDEFINED_VALUE) {
            mCirclePaint.setColor(mBackgroundColor);
        }
        mCenterImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        addView(mCenterImage);

        mCenterImage.setOnTouchListener(new CenterImageTouchListener());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        mRecyclerView.setHasItemsToScroll(mLayoutManager.isCountOfItemsAvailableToScroll());
        return super.onInterceptTouchEvent(ev);
    }

    /**
     * Set menu item click listener
     *
     * @param onMenuItemClickListener listener
     */
    public void setOnMenuItemClickListener(@Nullable OnMenuItemClickListener onMenuItemClickListener) {
        mAdapter.setOnMenuItemClickListener(onMenuItemClickListener);
    }

    /**
     * Add the menu item.
     *
     * @param item menu item to add
     */
    public void addMenuItem(@NonNull CycleMenuItem item) {
        checkNonNullParams(item, FIELD_NAME_FOR_EXCEPTION_ITEM);
        mInitialized = false;
        mAdapter.addItem(item);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Set the menu items from the menu res.
     *
     * @param menuResId menu resource from which need to get menuItems and add to the cycleMenu
     */
    @SuppressLint("RestrictedApi")
    public void setMenuRes(@MenuRes int menuResId) {
        mInitialized = false;
        Menu menu = new MenuBuilder(getContext());
        MenuInflater inflater = new MenuInflater(getContext());
        inflater.inflate(menuResId, menu);
        setMenu(menu);
    }

    /**
     * Set the menu items from the Menu object
     *
     * @param menu menu object from which need to get menuItems and add to the cycleMenu
     */
    public void setMenu(@NonNull Menu menu) {
        checkNonNullParams(menu, FIELD_NAME_FOR_EXCEPTION_MENU);
        mInitialized = false;
        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            CycleMenuItem cycleMenuItem = new CycleMenuItem(menuItem.getItemId(), menuItem.getIcon());
            mAdapter.addItem(cycleMenuItem);
        }
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Add the menu items for the cycleMenu
     *
     * @param items Collection of the items to add
     */
    public void addMenuItems(@NonNull Collection<CycleMenuItem> items) {
        checkNonNullParams(items, FIELD_NAME_FOR_EXCEPTION_ITEMS);
        mInitialized = false;
        mAdapter.addItems(items);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Set the menu items for the cycleMenu
     *
     * @param items Collection of the items to set
     */
    public void setMenuItems(@NonNull Collection<CycleMenuItem> items) {
        checkNonNullParams(items, FIELD_NAME_FOR_EXCEPTION_ITEMS);
        mInitialized = false;
        mAdapter.setItems(items);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Set the scaling type which will be used to calculate radius for the cycle menu.
     *
     * @param corner - mCorner to set for the menu LEFT_TOP, RIGHT_TOP, LEFT_BOTTOM, RIGHT_BOTTOM
     */
    public void setCorner(@NonNull CORNER corner) {
        checkNonNullParams(corner, FIELD_NAME_FOR_EXCEPTION_CORNER);
        mInitialized = false;
        mLayoutManager.setCorner(corner);
        mCorner = corner;
    }

    /**
     * Set the scaling type which will be used to calculate radius for the cycle menu.
     *
     * @param scalingType type of scaling AUTO,FIXED
     */
    public void setScalingType(@NonNull RADIUS_SCALING_TYPE scalingType) {
        checkNonNullParams(scalingType, FIELD_NAME_FOR_EXCEPTION_SCALING_TYPE);
        mInitialized = false;
        mScalingType = scalingType;
    }

    /**
     * Set ripple color
     *
     * @param rippleColor color to set
     */
    public void setRippleColor(int rippleColor) {
        mRippleColor = rippleColor;
        setRippleAlpha(Color.alpha(mRippleColor));
        mRipplePaint.setColor(mRippleColor);
    }

    /**
     * Set image for corner image button
     *
     * @param cornerImageDrawable resource to drawable
     */
    public void setCornerImageDrawable(@Nullable Drawable cornerImageDrawable) {
        mCenterImage.setImageDrawable(cornerImageDrawable);
    }

    /**
     * Set image resource for corner image button
     *
     * @param drawableRes resource to drawable
     */
    public void setCornerImageResource(@DrawableRes int drawableRes) {
        mCenterImage.setImageResource(drawableRes);
    }

    /**
     * Set image bitmap for corner image button
     *
     * @param bitmap resource to drawable
     */
    public void setCornerImageBitmap(Bitmap bitmap) {
        mCenterImage.setImageBitmap(bitmap);
    }

    /**
     * Applies a min radius radius for the menu. Will be used if scaling_type set to {@code RADIUS_SCALING_TYPE.AUTO}
     *
     * @param autoMinRadius min radius to set
     */
    public void setAutoMinRadius(int autoMinRadius) {
        mInitialized = false;
        mAutoMinRadius = autoMinRadius;
    }

    /**
     * Applies a max radius radius for the menu. Will be used if scaling_type set to {@code RADIUS_SCALING_TYPE.AUTO}
     *
     * @param autoMaxRadius max radius to set
     */
    public void setAutoMaxRadius(int autoMaxRadius) {
        mInitialized = false;
        mAutoMaxRadius = autoMaxRadius;
    }

    /**
     * Applies a fixed radius for the menu. Will be used if scaling_type set to {@code RADIUS_SCALING_TYPE.FIXED}
     *
     * @param fixedRadius - fixed radius to set
     */
    public void setFixedRadius(int fixedRadius) {
        mInitialized = false;
        mFixedRadius = fixedRadius;
    }

    /**
     * Applies a radius of the collapsed menu.
     *
     * @param collapsedRadius radius to set (in px)
     */
    public void setCollapsedRadius(int collapsedRadius)
            throws IllegalArgumentException {
        mInitialized = false;
        if (mScalingType == RADIUS_SCALING_TYPE.FIXED && collapsedRadius < mFixedRadius
                || mScalingType == RADIUS_SCALING_TYPE.AUTO && collapsedRadius < mAutoMaxRadius) {
            mCircleMinRadius = collapsedRadius;
        }
    }

    /**
     * Applies a color to the CycleMenuWidget background
     *
     * @param backgroundColor color to set
     */
    public void setBackground(int backgroundColor) {
        mBackgroundColor = backgroundColor;
        mCirclePaint.setColor(mBackgroundColor);
        invalidate();
    }

    /**
     * Applies a tint to the background drawable of the items in cycle menu. Does not modify the current tint
     * mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
     *
     * @param itemsBackgroundTint the tint to apply, may be {@code null} to clear tint
     */
    public void setItemsBackgroundTint(@Nullable ColorStateList itemsBackgroundTint) {
        mItemsBackgroundTint = itemsBackgroundTint;
        mAdapter.setItemsBackgroundTint(itemsBackgroundTint);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setupMargins();
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int parentWidth = ((ViewGroup) getParent()).getWidth();
        int parentHeight = ((ViewGroup) getParent()).getHeight();

        int newWidthMeasureSpec = widthMeasureSpec;
        int newHeightMeasureSpec = heightMeasureSpec;
        if (height == 0) {
            newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(parentHeight, MeasureSpec.AT_MOST);
            height = MeasureSpec.getSize(newHeightMeasureSpec);
        }

        if (width == 0) {
            newWidthMeasureSpec = MeasureSpec.makeMeasureSpec(parentWidth, MeasureSpec.AT_MOST);
            width = MeasureSpec.getSize(newWidthMeasureSpec);
        }

        if (mItemSize <= 0) {
            FloatingActionButton buttonItem = (FloatingActionButton) LayoutInflater.from(getContext()).inflate(R.layout.cm_item_fab, this, false);
            int buttonSpec = MeasureSpec.makeMeasureSpec(1000, MeasureSpec.AT_MOST);
            measureChild(buttonItem, buttonSpec, buttonSpec);
            int measuredItemWidth = buttonItem.getMeasuredWidth();
            int measuredItemHeight = buttonItem.getMeasuredHeight();
            mItemSize = measuredItemWidth > measuredItemHeight ? measuredItemWidth : measuredItemHeight;
            if (mItemSize > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mItemSize = (int) (mItemSize * 1.3);
                } else {
                    mItemSize = (int) (mItemSize - mPreLollipopAdditionalButtonsMargin * 2 / 1.5f);
                }
            }
        }

        mRecyclerSize = (int) ((width > height ? height : width) - mShadowSize);
        @SuppressWarnings("Range") int recyclerSizeMeasureSpec = MeasureSpec.makeMeasureSpec(mRecyclerSize, MeasureSpec.EXACTLY);

        if ((mScalingType == RADIUS_SCALING_TYPE.FIXED || mAutoMaxRadius > mRecyclerSize || mAutoMaxRadius < 0) && mRecyclerSize > 0) {
            mAutoMaxRadius = mRecyclerSize;
        }
        if (mAutoMinRadius < mCircleMinRadius + mItemSize) {
            mAutoMinRadius = mCircleMinRadius + mItemSize;
        }
        if (mAutoMinRadius > mAutoMaxRadius) {
            mAutoMinRadius = mAutoMaxRadius;
        }
        if (mScalingType == RADIUS_SCALING_TYPE.AUTO) {
            mRecyclerSize = (int) (mItemSize * mAdapter.getRealItemsCount() * 4 / (Math.PI * 2));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mRecyclerSize += mItemSize * 5 / 8;
            } else {
                mRecyclerSize += mItemSize * 7 / 8;
            }
            if (mRecyclerSize > mAutoMaxRadius) {
                mRecyclerSize = mAutoMaxRadius;
            }

            if (mRecyclerSize < mAutoMinRadius) {
                mRecyclerSize = mAutoMinRadius;
            }
        } else if (mRecyclerSize > 0) {
            if (mFixedRadius > mAutoMaxRadius) {
                mFixedRadius = mAutoMaxRadius;
            }
            if (mFixedRadius < mAutoMinRadius) {
                mFixedRadius = mAutoMinRadius;
            }
            mRecyclerSize = mFixedRadius;
        }

        mOutCircleRadius = mRecyclerSize;
        mRecyclerView.measure(recyclerSizeMeasureSpec, recyclerSizeMeasureSpec);

        int lCenterIconSize = (int) Math.sqrt(mCircleMinRadius * mCircleMinRadius / 2.0);
        int centerImageMeasureWidthSpec = MeasureSpec.makeMeasureSpec(lCenterIconSize, MeasureSpec.EXACTLY);
        int centerImageMeasureHeightSpec = MeasureSpec.makeMeasureSpec(lCenterIconSize, MeasureSpec.EXACTLY);
        mCenterImage.measure(centerImageMeasureWidthSpec, centerImageMeasureHeightSpec);

        width = resolveSize(width, newWidthMeasureSpec);
        height = resolveSize(height, newHeightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int containerWidth = r - l;
        int centerImageLeft = 0;
        int centerImageTop = 0;
        int centerImageRight = 0;
        int centerImageBottom = 0;
        int recyclerLeft = 0;
        int recyclerTop = 0;
        int recyclerRight = 0;
        int recyclerBottom = 0;

        if (mCorner.isUpSide()) {
            centerImageBottom = mCenterImage.getMeasuredHeight();
            recyclerTop = t;
            recyclerBottom = t + mRecyclerSize;
        } else if (mCorner.isBottomSide()) {
            centerImageTop = getHeight() - mCenterImage.getMeasuredHeight();
            centerImageBottom = getHeight();
            recyclerTop = b - mRecyclerSize;
            recyclerBottom = b;
        }
        if (mCorner.isLeftSide()) {
            centerImageRight = mCenterImage.getMeasuredWidth();
            recyclerLeft = l;
            recyclerRight = l + mRecyclerSize;
        } else if (mCorner.isRightSide()) {
            centerImageLeft = containerWidth - mCenterImage.getMeasuredWidth();
            centerImageRight = containerWidth;
            recyclerLeft = r - mRecyclerSize;
            recyclerRight = r;
        }

        mCenterImage.layout(centerImageLeft, centerImageTop, centerImageRight, centerImageBottom);
        mRecyclerView.layout(recyclerLeft, recyclerTop, recyclerRight, recyclerBottom);
        mRecyclerView.setTranslationX(getWidth());
        int countOfVisibleElements = (int) ((mRecyclerSize * Math.PI / 2) / mItemSize);
        if (!mInitialized && r > 0 && b > 0) {
            if (mAdapter.getRealItemsCount() > countOfVisibleElements && mScrollType == SCROLL.ENDLESS) {
                mAdapter.setScrollType(SCROLL.ENDLESS);
                if (mCurrentPosition == RecyclerView.NO_POSITION) {
                    mCurrentPosition = Integer.MAX_VALUE / 2 + (mAdapter.getRealItemsCount() - Integer.MAX_VALUE / 2 % mAdapter.getRealItemsCount());
                }
            } else {
                mAdapter.setScrollType(SCROLL.BASIC);
            }
            if (mCurrentPosition != RecyclerView.NO_POSITION) {
                mLayoutManager.scrollToPosition(mCurrentPosition);
            }
            mLayoutManager.setAdditionalAngleOffset(mCurrentAngleOffset);
            mRecyclerView.post(runnableRequestLayout);
            mInitialized = true;
        }
        if (mState == STATE.OPEN) {
            open(false);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int mainCircleRadius = mAnimationCircleRadius;
        buildShadowCorners();

        int rippleRadius = mainCircleRadius < mRippleRadius ? mainCircleRadius : mRippleRadius;
        int circleCenterX = 0;
        int circleCenterY = 0;
        if (mCorner == CORNER.LEFT_TOP) {
            int canvasState = canvas.save();
            canvas.rotate(-90, getWidth(), 0);
            canvas.translate(0, -getWidth());
            canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
            canvas.restoreToCount(canvasState);
        } else if (mCorner == CORNER.RIGHT_TOP) {
            circleCenterX = canvas.getWidth();
            circleCenterY = 0;
            canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
        } else if (mCorner == CORNER.LEFT_BOTTOM) {
            circleCenterX = 0;
            circleCenterY = getHeight();
            int canvasState = canvas.save();
            canvas.rotate(-180, getWidth(), 0);
            canvas.translate(getWidth(), -getHeight());
            canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
            canvas.restoreToCount(canvasState);
        } else if (mCorner == CORNER.RIGHT_BOTTOM) {
            circleCenterX = getWidth();
            circleCenterY = getHeight();
            int canvasState = canvas.save();
            canvas.rotate(90, getWidth(), 0);
            canvas.translate(getHeight(), 0);
            canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
            canvas.restoreToCount(canvasState);
        }

        canvas.drawCircle(circleCenterX, circleCenterY, mainCircleRadius, mCirclePaint);
        canvas.drawCircle(
                circleCenterX,
                circleCenterY,
                rippleRadius,
                mRipplePaint);
    }

    /**
     * Build path for circular shadow.
     */
    private void buildShadowCorners() {
        float mCornerRadius = mAnimationCircleRadius;

        RectF innerBounds = new RectF(getWidth() - mCornerRadius, -mCornerRadius, getWidth() + mCornerRadius, mCornerRadius);
        RectF outerBounds = new RectF(innerBounds);
        outerBounds.inset(-mVariableShadowSize, -mVariableShadowSize);

        if (mCornerShadowPath == null) {
            mCornerShadowPath = new Path();
        } else {
            mCornerShadowPath.reset();
        }
        mCornerShadowPath.setFillType(Path.FillType.EVEN_ODD);
        mCornerShadowPath.moveTo(getWidth() - mCornerRadius, 0);

        mCornerShadowPath.rLineTo(-mVariableShadowSize, 0);
        // outer arc
        mCornerShadowPath.arcTo(outerBounds, 180f, -90f, false);
        // inner arc
        mCornerShadowPath.arcTo(innerBounds, 90f, 90f, false);

        float shadowRadius = -outerBounds.top;
        if (shadowRadius > 0f) {
            float startRatio = mCornerRadius / shadowRadius;
            float midRatio = startRatio + ((1f - startRatio) / 2f);
            RadialGradient gradient = new RadialGradient(getWidth(), 0, shadowRadius,
                    new int[]{0, mShadowStartColor, mShadowMiddleColor, mShadowEndColor},
                    new float[]{0f, startRatio, midRatio, 1f},
                    Shader.TileMode.CLAMP);
            mCornerShadowPaint.setShader(gradient);
        }
    }

    /**
     * Set scroll type for menu
     *
     * @param scrollType the scroll type BASIC, ENDLESS
     */
    public void setScrollType(@NonNull SCROLL scrollType) {
        checkNonNullParams(scrollType, FIELD_NAME_FOR_EXCEPTION_SCROLLING_TYPE);
        mScrollType = scrollType;
    }

    /**
     * Retrieve current position from the menu
     *
     * @return position of the first item
     */
    private int getCurrentPosition() {
        return mLayoutManager.getCurrentPosition();
    }

    /**
     * Retrieve current offset as an angle (degree) of the first item
     *
     * @return offset as an angle of the first angle
     */
    private double getCurrentItemsAngleOffset() {
        return mCurrentAngleOffset;
    }

    /**
     * Set current position of the menu to be first
     *
     * @param position - position of the first item
     */
    public void setCurrentPosition(int position) {
        if (position != RecyclerView.NO_POSITION) {
            mCurrentPosition = position;
        }
    }

    /**
     * Set current offset of the firstItem as an angle (in degrees)
     *
     * @param angle - offset that need to set for the first item (and next) in degrees
     */
    public void setCurrentItemsAngleOffset(double angle) {
        mCurrentAngleOffset = angle;
        mLayoutManager.setAdditionalAngleOffset(angle);
    }

    /**
     * Set StateChangeListener to MenuWidget
     *
     * @param listener OnStateChangedListener
     */
    public void setStateChangeListener(@Nullable OnStateChangedListener listener) {
        mOnStateChangeListener = listener;
    }

    /**
     * Set mState save listener. this object widget will call to save position of the first item and offset in degrees.
     *
     * @param stateSaveListener - listener
     */
    public void setStateSaveListener(StateSaveListener stateSaveListener) {
        mStateSaveListener = stateSaveListener;
    }

    @SuppressWarnings("unused")
    private void setRippleAlpha(int rippleAlpha) {
        mRipplePaint.setAlpha(rippleAlpha);
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mInitialized = false;
        mLayoutManager.requestLayout();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDetachedFromWindow() {
        mCurrentPosition = getCurrentPosition();
        mCurrentAngleOffset = mLayoutManager.getCurrentItemsAngleOffset();
        if (mStateSaveListener != null) {
            mStateSaveListener.saveState(mCurrentPosition, mCurrentAngleOffset);
        }
        if (mState == STATE.IN_CLOSE_PROCESS) {
            close(false);
        }
        if (mState == STATE.IN_OPEN_PROCESS) {
            mState = STATE.OPEN;
            sendState();
            mAnimationCircleRadius = mOutCircleRadius;
            scrollEnabled(true);
        }
        super.onDetachedFromWindow();
    }

    private void sendState() {
        if (mOnStateChangeListener != null) {
            mOnStateChangeListener.onStateChanged(mState);
        }
    }

    private void setupMargins() {
        MarginLayoutParams params = (MarginLayoutParams) getLayoutParams();
        if (params == null) {
            return;
        }

        switch (mCorner) {
            case LEFT_TOP:
                params.leftMargin = 0;
                params.topMargin = 0;
                break;
            case LEFT_BOTTOM:
                params.leftMargin = 0;
                params.bottomMargin = 0;
                break;
            case RIGHT_BOTTOM:
                params.rightMargin = 0;
                params.bottomMargin = 0;
                break;
            case RIGHT_TOP:
                params.rightMargin = 0;
                params.topMargin = 0;
                break;
        }
        setLayoutParams(params);
    }

    /**
     * enable/disable of items scrolling.
     *
     * @param enabled - scroll enabling value
     */
    private void scrollEnabled(boolean enabled) {
        mRecyclerView.setTouchEnabled(enabled);
        mLayoutManager.setScrollEnabled(enabled);
    }

    /**
     * Change menu mState open -> close, close -> open if don't doing open/close right now.
     */
    private void changeMenuState() {
        if (mState == STATE.IN_OPEN_PROCESS || mState == STATE.IN_CLOSE_PROCESS) {
            return;
        }
        if (mState == STATE.OPEN) {
            close(true);
            return;
        }
        open(true);
    }

    /**
     * Open cycle menu.
     *
     * @param animated - indicate if need to open cycle menu with animation (true), immediately otherwise
     */
    public void open(final boolean animated) {
        int centerCrossImageRotateAngle = -45;
        if (animated) {
            scrollEnabled(false);
            mState = STATE.IN_OPEN_PROCESS;
            sendState();
            mCenterImage.animate()
                    .rotation(centerCrossImageRotateAngle)
                    .setInterpolator(new OvershootInterpolator(2))
                    .setDuration(CENTER_IMAGE_ROTATE_DURATION)
                    .start();

            ObjectAnimator circleRadiusAnimator = ObjectAnimator.ofInt(this, CIRCLE_RADIUS_ANIMATOR_FIELD_NAME, mCircleMinRadius, mOutCircleRadius);
            circleRadiusAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mRecyclerView.setTranslationX(0);
                    mLayoutManager.rollInItemsWithAnimation(new CycleLayoutManager.OnCompleteCallback() {
                        @Override
                        public void onComplete() {
                            mState = STATE.OPEN;
                            sendState();
                            scrollEnabled(true);
                            if (mOnStateChangeListener != null) {
                                mOnStateChangeListener.onOpenComplete();
                            }
                        }
                    });
                }
            });
            ObjectAnimator shadowAnimator = ObjectAnimator.ofFloat(this, SHADOW_SIZE_ANIMATOR_FIELD_NAME, mShadowSize * SHADOW_SIZE_MIN_COEFFICIENT, mShadowSize);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.setDuration(REVEAL_ANIMATION_DURATION);
            animatorSet.playTogether(circleRadiusAnimator, shadowAnimator);
            animatorSet.start();
        } else {
            mVariableShadowSize = mShadowSize;
            mCenterImage.setRotation(centerCrossImageRotateAngle);
            mAnimationCircleRadius = mOutCircleRadius;
            mRecyclerView.setTranslationX(0);
            scrollEnabled(true);
            mState = STATE.OPEN;
            sendState();
            invalidate();
        }
    }

    /**
     * Close cycle menu.
     *
     * @param animated - indicate if need to close cycle menu with animation (true), immediately otherwise
     */
    public void close(boolean animated) {
        if (animated) {
            scrollEnabled(false);
            mState = STATE.IN_CLOSE_PROCESS;
            sendState();
            mCenterImage.animate()
                    .rotation(0)
                    .setInterpolator(new OvershootInterpolator(2))
                    .setDuration(CENTER_IMAGE_ROTATE_DURATION)
                    .start();
            mLayoutManager.rollOutItemsWithAnimation(new CycleLayoutManager.OnCompleteCallback() {
                @Override
                public void onComplete() {
                    innerAnimatedClose();
                }
            });
        } else {
            scrollEnabled(true);
            mState = STATE.CLOSED;
            sendState();
            mVariableShadowSize = mShadowSize * SHADOW_SIZE_MIN_COEFFICIENT;
            mCenterImage.setRotation(0);
            mAnimationCircleRadius = mCircleMinRadius;
            invalidate();
        }
    }

    private void innerAnimatedClose() {
        ObjectAnimator circleRadiusAnimator = ObjectAnimator.ofInt(this, CIRCLE_RADIUS_ANIMATOR_FIELD_NAME, mOutCircleRadius, mCircleMinRadius);
        circleRadiusAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mState = STATE.CLOSED;
                sendState();
                if (mOnStateChangeListener != null) {
                    mOnStateChangeListener.onCloseComplete();
                }
            }
        });
        ObjectAnimator shadowAnimator = ObjectAnimator.ofFloat(this, SHADOW_SIZE_ANIMATOR_FIELD_NAME, mShadowSize, mShadowSize * SHADOW_SIZE_MIN_COEFFICIENT);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(REVEAL_ANIMATION_DURATION);
        animatorSet.playTogether(circleRadiusAnimator, shadowAnimator);
        animatorSet.start();
        mRecyclerView.setTranslationX(getWidth());
    }

    @SuppressWarnings("unused")
    private void setRippleRadius(int rippleRadius) {
        mRippleRadius = rippleRadius;
        if (mShouldOpen && rippleRadius >= mCircleMinRadius) {
            mShouldOpen = false;
            changeMenuState();
        }
        invalidate();
    }

    @SuppressWarnings("unused")
    private void setVariableShadowSize(float variableShadowSize) {
        mVariableShadowSize = variableShadowSize;
    }

    @SuppressWarnings("unused")
    private void setAnimationCircleRadius(int animationCircleRadius) {
        mAnimationCircleRadius = animationCircleRadius;
        invalidate();
    }

    private class CenterImageTouchListener implements OnTouchListener {
        private boolean wasOutside = false;
        private Rect rect = new Rect();
        private ObjectAnimator mRippleSizeAnimator;
        private ObjectAnimator mRippleAlphaAnimator;

        private void cancelRippleAnimator() {
            if (mRippleSizeAnimator != null) {
                mRippleSizeAnimator.cancel();
            }
            if (mRippleAlphaAnimator != null) {
                mRippleAlphaAnimator.cancel();
            }
        }

        private void startRippleSizeAnimator(int fromRadius, int toRadius) {
            mRippleSizeAnimator = ObjectAnimator.ofInt(CycleMenuWidget.this, RIPPLE_RADIUS_ANIMATOR_FIELD_NAME, fromRadius, toRadius).setDuration(RIPPLE_REVEAL_DURATION);
            mRippleSizeAnimator.start();
        }

        private void startRippleAlphaAnimator(int fromAlpha, int toAlpha) {
            mRippleAlphaAnimator = ObjectAnimator.ofInt(CycleMenuWidget.this, RIPPLE_ALPHA_ANIMATOR_FIELD_NAME, fromAlpha, toAlpha).setDuration(RIPPLE_ALPHA_DURATION);
            mRippleAlphaAnimator.start();
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mShouldOpen = false;
            if (mState == STATE.IN_OPEN_PROCESS || mState == STATE.IN_CLOSE_PROCESS) {
                return false;
            }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    cancelRippleAnimator();
                    rect.set(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
                    wasOutside = false;
                    setRippleAlpha(Color.alpha(mRippleColor));
                    startRippleSizeAnimator(0, mAnimationCircleRadius);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!rect.contains(v.getLeft() + (int) event.getX(), v.getTop() + (int) event.getY())) {
                        wasOutside = true;
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    wasOutside = true;
                case MotionEvent.ACTION_UP:
                    cancelRippleAnimator();
                    if (wasOutside) {
                        startRippleSizeAnimator(mAnimationCircleRadius, 0);
                    } else {
                        if (mAnimationCircleRadius == mRippleRadius) {
                            mRippleRadius = mOutCircleRadius;
                            changeMenuState();
                        } else {
                            startRippleSizeAnimator(mRippleRadius, mOutCircleRadius);
                            if (mState == STATE.CLOSED) {
                                mShouldOpen = true;
                            } else {
                                changeMenuState();
                            }
                        }
                    }
                    startRippleAlphaAnimator(Color.alpha(mRippleColor), 0);
                    break;
                default:
            }
            return true;
        }
    }

    private void checkNonNullParams(Object param, String paramName) {
        if (param == null) {
            throw new IllegalArgumentException("Parameter \"" + paramName + "\" can't be null.");
        }
    }
}
