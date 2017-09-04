package net.frju.flym.utils

import android.support.design.widget.Snackbar
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import net.fred.feedex.R
import net.frju.flym.GlideApp
import java.net.URL

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

fun ImageView.loadFavicon(feedLink: String) {
    try {
        val domain = URL(feedLink).host
        GlideApp.with(context).load("https://www.google.com/s2/favicons?domain=$domain").error(R.mipmap.ic_launcher).into(this)
    } catch (_: Throwable) {
        GlideApp.with(context).load(R.mipmap.ic_launcher).into(this)
    }
}

/**
 * Display Snackbar with the [Snackbar.LENGTH_INDEFINITE] duration.
 *
 * @param message the message text resource.
 */
fun View.indefiniteSnackbar(message: Int, actionText: Int, action: (View) -> Unit) = Snackbar
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
fun View.indefiniteSnackbar(message: String, actionText: String, action: (View) -> Unit) = Snackbar
        .make(this, message, Snackbar.LENGTH_INDEFINITE)
        .apply {
            setAction(actionText, action)
            show()
        }