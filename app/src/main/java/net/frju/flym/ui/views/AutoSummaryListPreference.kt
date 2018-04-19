package net.frju.flym.ui.views

import android.content.Context
import android.util.AttributeSet

class AutoSummaryListPreference(context: Context, attrs: AttributeSet) : android.preference.ListPreference(context, attrs) {

	override fun onDialogClosed(positiveResult: Boolean) {
		super.onDialogClosed(positiveResult)
		if (positiveResult) {
			summary = entry
		}
	}

	override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
		super.onSetInitialValue(restoreValue, defaultValue)
		summary = entry
	}
}