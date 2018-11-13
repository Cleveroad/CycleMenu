package com.cleveroad.cyclemenusample;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.cleveroad.sy.cyclemenuwidget.CycleMenuWidget;
import com.cleveroad.sy.cyclemenuwidget.OnMenuItemClickListener;
import com.cleveroad.sy.cyclemenuwidget.OnStateChangedListener;
import com.cleveroad.sy.cyclemenuwidget.StateSaveListener;

import java.util.Arrays;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ItemHolder> implements OnItemCycleMenuStateChangedListener, StateSaver {

    private static final int COUNT = 20;

    private Context mContext;
    private boolean[] mMenuStatesOpen = {true, true, false, true, false, true, false, true, false, true, true, true, false, true, false, true, false, true, false, true,};

    private int[] mPositions = new int[COUNT];
    private double[] mAngles = new double[COUNT];


    RecyclerViewAdapter(Context context) {
        mContext = context;
        Arrays.fill(mAngles, CycleMenuWidget.UNDEFINED_ANGLE_VALUE);
        Arrays.fill(mPositions, RecyclerView.NO_POSITION);
    }

    @Override
    public int getItemViewType(int position) {
        return position % 5;
    }

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType != 4) {
            view = LayoutInflater.from(mContext).inflate(R.layout.item_example, parent, false);
            CycleMenuWidget cycleMenuWidget = (CycleMenuWidget) view.findViewById(R.id.itemCycleMenuWidget);
            switch (viewType) {
                case 0:
                    cycleMenuWidget.setMenuRes(R.menu.cycle_menu_22);
                    cycleMenuWidget.setCorner(CycleMenuWidget.CORNER.LEFT_TOP);
                    break;
                case 1:
                    cycleMenuWidget.setMenuRes(R.menu.cycle_menu_3);
                    cycleMenuWidget.setCorner(CycleMenuWidget.CORNER.LEFT_BOTTOM);
                    break;
                case 2:
                    cycleMenuWidget.setMenuRes(R.menu.cycle_menu_4);
                    break;
                default:
                    cycleMenuWidget.setMenuRes(R.menu.cycle_menu_5);
                    cycleMenuWidget.setCorner(CycleMenuWidget.CORNER.RIGHT_BOTTOM);
                    break;
            }
        } else {
            view = LayoutInflater.from(mContext).inflate(R.layout.item_example_2, parent, false);
            RelativeLayout layout = (RelativeLayout) view;
            CycleMenuWidget cycleMenuWidget = new CycleMenuWidget(mContext);
            cycleMenuWidget.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            cycleMenuWidget.setId(R.id.itemCycleMenuWidget);
            cycleMenuWidget.setMenuRes(R.menu.cycle_menu_22);
            cycleMenuWidget.setCorner(CycleMenuWidget.CORNER.LEFT_BOTTOM);
            cycleMenuWidget.setRippleColor(Color.argb(100, 200, 100, 100));
            layout.addView(cycleMenuWidget);
        }
        return new ItemHolder(view, this, this);
    }

    @Override
    public void onBindViewHolder(RecyclerViewAdapter.ItemHolder holder, int position) {
        holder.mCycleMenuWidget.setCurrentPosition(mPositions[position]);
        holder.mCycleMenuWidget.setCurrentItemsAngleOffset(mAngles[position]);
        if (mMenuStatesOpen[position]) {
            holder.mCycleMenuWidget.open(false);
        } else {
            holder.mCycleMenuWidget.close(false);
        }
    }

    @Override
    public int getItemCount() {
        return COUNT;
    }

    @Override
    public void onOpen(int position) {
        Log.e("onOpenComplete", "onOpenComplete " + position + " " + mMenuStatesOpen.length);
        if (position >= 0 && position < mMenuStatesOpen.length) {
            mMenuStatesOpen[position] = true;
        }
    }

    @Override
    public void onClose(int position) {
        Log.e("onCloseComplete", "onCloseComplete " + position + " " + mMenuStatesOpen.length);
        if (position >= 0 && position < mMenuStatesOpen.length) {
            mMenuStatesOpen[position] = false;
        }
    }

    @Override
    public void saveState(int adapterPosition, int itemPosition, double lastItemAngleShift) {
        mPositions[adapterPosition] = itemPosition;
        mAngles[adapterPosition] = lastItemAngleShift;
    }


    static class ItemHolder extends RecyclerView.ViewHolder implements OnStateChangedListener, StateSaveListener, OnMenuItemClickListener {

        private CycleMenuWidget mCycleMenuWidget;
        private OnItemCycleMenuStateChangedListener mListener;
        private StateSaver saver;

        ItemHolder(View itemView, OnItemCycleMenuStateChangedListener listener, StateSaver saver) {
            super(itemView);
            this.mListener = listener;
            mCycleMenuWidget = (CycleMenuWidget) itemView.findViewById(R.id.itemCycleMenuWidget);
            mCycleMenuWidget.setStateChangeListener(this);
            mCycleMenuWidget.setStateSaveListener(this);
            mCycleMenuWidget.setOnMenuItemClickListener(this);
            mCycleMenuWidget.setScalingType(CycleMenuWidget.RADIUS_SCALING_TYPE.FIXED);
            this.saver = saver;
        }

        @Override
        public void saveState(int itemPosition, double lastItemAngleShift) {
            if (saver != null) {
                saver.saveState(getAdapterPosition(), itemPosition, lastItemAngleShift);
            }
        }

        @Override
        public void onStateChanged(CycleMenuWidget.STATE state) {
            if (mListener == null) {
                return;
            }
            //Will save state in process because user can scroll to next item and onOpenComplete or onCloseComplete will not be received due to reuse of holder.
            if (state == CycleMenuWidget.STATE.IN_OPEN_PROCESS) {
                mListener.onOpen(getAdapterPosition());
            }
            if (state == CycleMenuWidget.STATE.IN_CLOSE_PROCESS) {
                mListener.onClose(getAdapterPosition());
            }
        }

        @Override
        public void onOpenComplete() {

        }

        @Override
        public void onCloseComplete() {

        }


        @Override
        public void onMenuItemClick(View view, int itemPosition) {
            Log.i("onMenuItemClick", "Click view id = " + view.getId() + " itemPosition = " + itemPosition);
            Toast.makeText(view.getContext(), "Click id = " + view.getId() + " itemPosition = " + itemPosition, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onMenuItemLongClick(View view, int itemPosition) {
            Log.i("onMenuItemLongClick", "LongClick view id = " + view.getId() + " itemPosition = " + itemPosition);
            Toast.makeText(view.getContext(), "LongClick id = " + view.getId() + " itemPosition = " + itemPosition, Toast.LENGTH_SHORT).show();
        }
    }


}
