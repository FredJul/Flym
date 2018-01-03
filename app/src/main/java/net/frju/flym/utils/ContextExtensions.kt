package net.frju.flym.utils

import android.app.Activity
import android.content.Context
import org.jetbrains.anko.connectivityManager
import org.jetbrains.anko.inputMethodManager

fun Context.isOnline(): Boolean {
	val netInfo = connectivityManager.activeNetworkInfo
	if (netInfo != null && netInfo.isConnectedOrConnecting) {
		return true
	}
	return false
}

fun Activity.closeKeyboard() {
	currentFocus?.let {
		inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
	}
}
