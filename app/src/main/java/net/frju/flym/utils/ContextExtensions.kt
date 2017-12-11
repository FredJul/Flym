package net.frju.flym.utils

import android.content.Context
import org.jetbrains.anko.connectivityManager

fun Context.isOnline(): Boolean {
	val netInfo = connectivityManager.activeNetworkInfo
	if (netInfo != null && netInfo.isConnectedOrConnecting) {
		return true
	}
	return false
}
