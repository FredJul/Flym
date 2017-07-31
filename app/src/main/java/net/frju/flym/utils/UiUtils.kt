package net.frju.flym.utils

import android.view.View
import android.view.ViewTreeObserver

/**
 * Executes the given [java.lang.Runnable] when the view is laid out
 */
fun View.onLaidOut(runnable: () -> Unit) {
    if (isLaidOut) {
        runnable()
        return
    }

    val observer = viewTreeObserver
    observer.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            val trueObserver: ViewTreeObserver

            if (observer.isAlive) {
                trueObserver = observer
            } else {
                trueObserver = viewTreeObserver
            }

            trueObserver.removeOnGlobalLayoutListener(this)
            runnable()
        }
    })
}
