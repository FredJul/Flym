package net.frju.flym.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent?) {
		AutoRefreshJobService.initAutoRefresh(context)
	}
}
