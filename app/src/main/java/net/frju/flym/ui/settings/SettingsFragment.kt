package net.frju.flym.ui.settings

import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import net.fred.feedex.R
import net.frju.flym.data.utils.PrefUtils
import net.frju.flym.service.AutoRefreshJobService


class SettingsFragment : PreferenceFragment() {

	private val onRefreshChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
		AutoRefreshJobService.initAutoRefresh(activity)
		true
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		addPreferencesFromResource(R.xml.settings)

		findPreference(PrefUtils.REFRESH_ENABLED)?.onPreferenceChangeListener = onRefreshChangeListener
		findPreference(PrefUtils.REFRESH_INTERVAL)?.onPreferenceChangeListener = onRefreshChangeListener
	}
}