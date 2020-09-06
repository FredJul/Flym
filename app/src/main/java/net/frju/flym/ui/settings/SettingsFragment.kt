/*
 * Copyright (c) 2012-2018 Frederic Julian
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package net.frju.flym.ui.settings

import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import net.fred.feedex.R
import net.frju.flym.data.utils.PrefConstants.REFRESH_ENABLED
import net.frju.flym.data.utils.PrefConstants.REFRESH_INTERVAL
import net.frju.flym.data.utils.PrefConstants.THEME
import net.frju.flym.service.AutoRefreshJobService
import net.frju.flym.ui.main.MainActivity
import net.frju.flym.ui.views.AutoSummaryListPreference
import org.jetbrains.anko.support.v4.startActivity


class SettingsFragment : PreferenceFragmentCompat() {

    private val onRefreshChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
        AutoRefreshJobService.initAutoRefresh(requireContext())
        true
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        findPreference<CheckBoxPreference>(REFRESH_ENABLED)?.onPreferenceChangeListener = onRefreshChangeListener
        findPreference<AutoSummaryListPreference>(REFRESH_INTERVAL)?.onPreferenceChangeListener = onRefreshChangeListener

        findPreference<AutoSummaryListPreference>(THEME)?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, _ ->
                    activity?.finishAffinity()
                    startActivity<MainActivity>()
                    true
                }
    }
}