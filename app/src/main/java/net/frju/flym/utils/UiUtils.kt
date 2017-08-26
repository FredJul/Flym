package net.frju.flym.utils

import android.support.design.widget.Snackbar
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

/**
 * Display Snackbar with the [Snackbar.LENGTH_INDEFINITE] duration.
 *
 * @param message the message text resource.
 */
inline fun View.indefiniteSnackbar(message: Int, actionText: Int, noinline action: (View) -> Unit) = Snackbar
        .make(this, message, Snackbar.LENGTH_INDEFINITE)
        .apply {
            setAction(actionText, action)
            show()
        }

/**
 * Display Snackbar with the [Snackbar.LENGTH_INDEFINITE] duration.
 *
 * @param message the message text.
 */
inline fun View.indefiniteSnackbar(message: String, actionText: String, noinline action: (View) -> Unit) = Snackbar
        .make(this, message, Snackbar.LENGTH_INDEFINITE)
        .apply {
            setAction(actionText, action)
            show()
        }