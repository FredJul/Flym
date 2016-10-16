/**
 * Flym
 * <p/>
 * Copyright (c) 2012-2015 Frederic Julian
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.fred.feedex.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ExpandableListView;
import android.widget.ImageView;

public class DragNDropExpandableListView extends ExpandableListView {

    private boolean mDragMode;

    private int mStartPosition;
    private int mDragPointOffset; // Used to adjust drag view location

    private ImageView mDragView;

    private DragNDropListener mDragNDropListener;

    public DragNDropExpandableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setDragNDropListener(DragNDropListener l) {
        mDragNDropListener = l;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getActionIndex() != 0) {
            return super.onTouchEvent(ev);
        }

        final int action = ev.getAction();
        final int x = (int) ev.getX(0);
        final int y = (int) ev.getY(0);

        if (action == MotionEvent.ACTION_DOWN && x > getWidth() - 80) { // drag on the right part of the item only
            mDragMode = true;
        }

        if (!mDragMode) {
            return super.onTouchEvent(ev);
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mStartPosition = pointToPosition(x, y);
                if (mStartPosition != INVALID_POSITION) {
                    int mItemPosition = mStartPosition - getFirstVisiblePosition();
                    mDragPointOffset = y - getChildAt(mItemPosition).getTop();
                    mDragPointOffset -= ((int) ev.getRawY()) - y;
                    startDrag(mItemPosition, y);
                    drag(0, y);// replace 0 with x if desired
                }
                break;
            case MotionEvent.ACTION_MOVE:
                drag(0, y);// replace 0 with x if desired
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
            default:
                mDragMode = false;
                int endPosition = pointToPosition(x, y);
                stopDrag(mStartPosition - getFirstVisiblePosition());
                if (mDragNDropListener != null && mStartPosition != INVALID_POSITION && endPosition != INVALID_POSITION)
                    mDragNDropListener.onDrop(mStartPosition, endPosition);
                break;
        }

        return ev.getPointerCount() <= 1 || super.onTouchEvent(ev);
    }

    // move the drag view
    private void drag(int x, int y) {
        if (mDragView != null) {
            WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mDragView.getLayoutParams();
            layoutParams.x = x;
            layoutParams.y = y - mDragPointOffset;
            WindowManager mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            mWindowManager.updateViewLayout(mDragView, layoutParams);

            if (mDragNDropListener != null)
                mDragNDropListener.onDrag(x, y, null);// change null to "this" when ready to use
        }
    }

    // enable the drag view for dragging
    private void startDrag(int itemIndex, int y) {
        stopDrag(itemIndex);

        View item = getChildAt(itemIndex);
        if (item == null)
            return;

        if (mDragNDropListener != null)
            mDragNDropListener.onStartDrag(item);

        WindowManager.LayoutParams mWindowParams = new WindowManager.LayoutParams();
        mWindowParams.gravity = Gravity.TOP;
        mWindowParams.x = 0;
        mWindowParams.y = y - mDragPointOffset;

        mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        mWindowParams.format = PixelFormat.TRANSLUCENT;
        mWindowParams.windowAnimations = 0;

        Context context = getContext();

        // Create a copy of the drawing cache so that it does not get recycled
        // by the framework when the list tries to clean up memory
        ImageView v = new ImageView(context);
        item.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(item.getDrawingCache());
        item.setDrawingCacheEnabled(false);
        v.setBackgroundResource(android.R.color.holo_blue_dark);
        v.setImageBitmap(bitmap);

        WindowManager mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.addView(v, mWindowParams);
        mDragView = v;
    }

    // destroy drag view
    private void stopDrag(int itemIndex) {
        if (mDragView != null) {
            if (mDragNDropListener != null)
                mDragNDropListener.onStopDrag(getChildAt(itemIndex));
            mDragView.setVisibility(GONE);
            WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            wm.removeView(mDragView);
            mDragView.setImageDrawable(null);
            mDragView = null;
        }
    }

    @Override
    public long getItemIdAtPosition(int flatPos) {
        long packedPos = getExpandableListPosition(flatPos);
        if (ExpandableListView.getPackedPositionType(packedPos) == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            return getExpandableListAdapter().getGroupId(ExpandableListView.getPackedPositionGroup(packedPos));
        } else {
            return getExpandableListAdapter().getChildId(ExpandableListView.getPackedPositionGroup(packedPos), ExpandableListView.getPackedPositionChild(packedPos));
        }
    }
}
