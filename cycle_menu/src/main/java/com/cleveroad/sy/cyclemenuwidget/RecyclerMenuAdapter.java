package com.cleveroad.sy.cyclemenuwidget;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Inner adapter for menu mItems.
 */
class RecyclerMenuAdapter extends RecyclerView.Adapter<RecyclerMenuAdapter.ItemHolder> implements OnMenuItemClickListener {

    private List<CycleMenuItem> mItems;
    private ColorStateList mItemsBackgroundTint;
    private boolean defaultTintColorChanged = false;
    private OnMenuItemClickListener mOnMenuItemClickListener;

    private CycleMenuWidget.SCROLL mScrollType = CycleMenuWidget.SCROLL.BASIC;

    RecyclerMenuAdapter() {
        mItems = new ArrayList<>();
    }

    /**
     * Set scroll type for menu
     *
     * @param scrollType the scroll type BASIC, ENDLESS
     */
    void setScrollType(CycleMenuWidget.SCROLL scrollType) {
        mScrollType = scrollType;
    }

    /**
     * Set items Collection for the adapter
     *
     * @param items collections to be set to adapter
     */
    void setItems(Collection<CycleMenuItem> items) {
        mItems.clear();
        mItems.addAll(items);
    }

    /**
     * Set menu item click listener
     *
     * @param onMenuItemClickListener listener
     */
    void setOnMenuItemClickListener(OnMenuItemClickListener onMenuItemClickListener) {
        mOnMenuItemClickListener = onMenuItemClickListener;
    }

    /**
     * Applies a tint to the background drawable of the items in cycle menu. Does not modify the current tint
     * mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
     *
     * @param itemsBackgroundTint the tint to apply, may be {@code null} to clear tint
     */
    void setItemsBackgroundTint(ColorStateList itemsBackgroundTint) {
        defaultTintColorChanged = true;
        mItemsBackgroundTint = itemsBackgroundTint;
    }

    /**
     * Add items Collection to the adapter
     *
     * @param items collections that need to be added to adapter
     */
    void addItems(Collection<CycleMenuItem> items) {
        mItems.addAll(items);
    }

    /**
     * Add item to the adapter
     *
     * @param item that need to add to the adapter
     */
    void addItem(CycleMenuItem item) {
        mItems.add(item);
    }

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cm_item_fab, parent, false);
        if (defaultTintColorChanged) {
            //noinspection RedundantCast
            ((FloatingActionButton) view).setBackgroundTintList(mItemsBackgroundTint);
        }
        return new ItemHolder(view, this);
    }

    @Override
    public void onBindViewHolder(ItemHolder holder, final int position) {
        FloatingActionButton button = (FloatingActionButton) holder.itemView;
        button.setImageDrawable(mItems.get(getRealPosition(position)).getIcon());
        holder.itemView.setId(mItems.get(getRealPosition(position)).getId());
    }

    @Override
    public int getItemCount() {
        //if scrollType is ENDLESS then need to set infinite scrolling
        if (mScrollType == CycleMenuWidget.SCROLL.ENDLESS) {
            return Integer.MAX_VALUE;
        }
        return mItems.size();
    }

    int getRealItemsCount() {
        return mItems.size();
    }

    /**
     * Return real position of item in adapter. Is used when scrollType = ENDLESS.
     *
     * @param position position form adapter.
     * @return int realPosition of the item in adapter
     */
    private int getRealPosition(int position) {
        return position % mItems.size();
    }

    @Override
    public void onMenuItemClick(View view, int itemPosition) {
        if (mOnMenuItemClickListener != null) {
            mOnMenuItemClickListener.onMenuItemClick(view, getRealPosition(itemPosition));
        }
    }

    @Override
    public void onMenuItemLongClick(View view, int itemPosition) {
        if (mOnMenuItemClickListener != null) {
            mOnMenuItemClickListener.onMenuItemLongClick(view, getRealPosition(itemPosition));
        }
    }

    static class ItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private OnMenuItemClickListener mOnMenuItemClickListener;

        ItemHolder(View itemView, OnMenuItemClickListener listener) {
            super(itemView);
            mOnMenuItemClickListener = listener;
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            //Resend click to the outer menu item click listener with provided item position. if scrollType is ENDLESS need to getRealPosition from the position.
            mOnMenuItemClickListener.onMenuItemClick(view, getAdapterPosition());
        }

        @Override
        public boolean onLongClick(View view) {
            mOnMenuItemClickListener.onMenuItemLongClick(view, getAdapterPosition());
            return true;
        }
    }
}
