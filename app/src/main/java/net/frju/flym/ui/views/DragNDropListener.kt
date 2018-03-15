package net.frju.flym.ui.views

import android.view.View

/**
 * Implement to handle an item being dragged.
 *
 * @author Eric Harlow
 */
interface DragNDropListener {
    /**
     * Called when a drag starts.
     *
     * @param itemView the view of the item to be dragged i.e. the drag view
     */
    fun onStartDrag(itemView: View)

    /**
     * Called when a drag is to be performed.
     *
     * @param x        horizontal coordinate of MotionEvent.
     * @param y        vertical coordinate of MotionEvent.
     */
    fun onDrag(x: Float, y: Float)

    /**
     * Called when a drag stops. Any changes in onStartDrag need to be undone here so that the view can be used in the list again.
     *
     * @param itemView the view of the item to be dragged i.e. the drag view
     */
    fun onStopDrag(itemView: View)

    /**
     * Called when an item is to be dropped.
     *
     * @param posFrom index item started at.
     * @param posTo   index to place item at.
     */
    fun onDrop(posFrom: Int, posTo: Int)
}
