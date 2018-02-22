package net.frju.flym.ui.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import net.fred.feedex.R
import org.jetbrains.anko.dip
import org.jetbrains.anko.windowManager

class DragNDropRecyclerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : RecyclerView(context, attrs, defStyle) {

    private var dragMode: Boolean = false

    private var startPosition = 0
    private var dragPointOffset = 0F // Used to adjust drag view location

    private var dragView: ImageView? = null

    var dragNDropListener: DragNDropListener? = null

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionIndex != 0) {
            return super.onTouchEvent(ev)
        }

        var action = ev.action
        val x = ev.getX(0)
        val y = ev.getY(0)

        if (x > width - dip(50)) { // drag on the right part of the item only
            if (!dragMode && action == MotionEvent.ACTION_MOVE) {
                action = MotionEvent.ACTION_DOWN
            }

            if (action == MotionEvent.ACTION_DOWN) {
                dragMode = true
            }
        }

        if (!dragMode) {
            return super.onTouchEvent(ev)
        }

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                startPosition = getChildAdapterPosition(findChildViewUnder(x, y))
                if (startPosition != NO_POSITION) {
                    val mItemPosition = startPosition - (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                    dragPointOffset = y - getChildAt(mItemPosition).top
                    dragPointOffset -= ev.rawY - y
                    startDrag(mItemPosition, y)
                    drag(0F, y)// replace 0 with x if desired
                }
            }
            MotionEvent.ACTION_MOVE -> drag(0F, y)// replace 0 with x if desired
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                dragMode = false
                val endPosition = getChildAdapterPosition(findChildViewUnder(x, y))
                stopDrag(startPosition - (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition())
                if (startPosition != NO_POSITION && endPosition != NO_POSITION)
                    dragNDropListener?.onDrop(startPosition, endPosition)
            }
            else -> {
                dragMode = false
                val endPosition = getChildAdapterPosition(findChildViewUnder(x, y))
                stopDrag(startPosition - (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition())
                if (startPosition != NO_POSITION && endPosition != NO_POSITION)
                    dragNDropListener?.onDrop(startPosition, endPosition)
            }
        }

        return ev.pointerCount <= 1 || super.onTouchEvent(ev)
    }

    // move the drag view
    private fun drag(x: Float, y: Float) {
        dragView?.let { dragView ->
            val layoutParams = dragView.layoutParams as WindowManager.LayoutParams
            layoutParams.x = x.toInt()
            layoutParams.y = (y - dragPointOffset).toInt()
            context.windowManager.updateViewLayout(dragView, layoutParams)

            dragNDropListener?.onDrag(x, y)
        }
    }

    // enable the drag view for dragging
    private fun startDrag(itemIndex: Int, y: Float) {
        stopDrag(itemIndex)

        val item = getChildAt(itemIndex) ?: return

        dragNDropListener?.onStartDrag(item)

        val windowParams = WindowManager.LayoutParams().apply {
            gravity = Gravity.TOP
            x = 0
            this.y = (y - dragPointOffset).toInt()

            height = WindowManager.LayoutParams.WRAP_CONTENT
            width = WindowManager.LayoutParams.WRAP_CONTENT
            flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            format = PixelFormat.TRANSLUCENT
            windowAnimations = 0
        }

        // Create a copy of the drawing cache so that it does not get recycled
        // by the framework when the list tries to clean up memory
        val v = ImageView(context).apply {
            item.isDrawingCacheEnabled = true
            val bitmap = Bitmap.createBitmap(item.drawingCache)
            item.isDrawingCacheEnabled = false

            setBackgroundResource(R.color.colorAccent)
            setImageBitmap(bitmap)
        }

        context.windowManager.addView(v, windowParams)
        dragView = v
    }

    // destroy drag view
    private fun stopDrag(itemIndex: Int) {
        if (dragView != null) {
            dragNDropListener?.onStopDrag(getChildAt(itemIndex))
            dragView?.visibility = View.GONE
            context.windowManager.removeView(dragView)
            dragView?.setImageDrawable(null)
            dragView = null
        }
    }
}
