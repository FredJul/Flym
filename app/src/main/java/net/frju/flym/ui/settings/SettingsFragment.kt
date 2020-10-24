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

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.nononsenseapps.filepicker.FilePickerActivity
import com.nononsenseapps.filepicker.Utils
import kotlinx.coroutines.ObsoleteCoroutinesApi
import net.fred.feedex.R
import net.frju.flym.data.utils.PrefConstants.DECSYNC_ENABLED
import net.frju.flym.data.utils.PrefConstants.DECSYNC_FILE
import net.frju.flym.data.utils.PrefConstants.DECSYNC_USE_SAF
import net.frju.flym.data.utils.PrefConstants.REFRESH_ENABLED
import net.frju.flym.data.utils.PrefConstants.REFRESH_INTERVAL
import net.frju.flym.data.utils.PrefConstants.THEME
import net.frju.flym.service.AutoRefreshJobService
import net.frju.flym.ui.main.MainActivity
import net.frju.flym.ui.views.AutoSummaryListPreference
import net.frju.flym.utils.*
import org.decsync.library.DecsyncPrefUtils
import org.jetbrains.anko.support.v4.startActivity

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        private const val CHOOSE_DECSYNC_FILE = 0
        private const val PERMISSIONS_REQUEST_DECSYNC = 2
        const val EXTRA_SELECT_SAF_DIR = "select_saf_dir"
    }

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

        if (requireContext().getPrefBoolean(DECSYNC_USE_SAF, false)) {
            findPreference<Preference>(DECSYNC_ENABLED)?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                if (newValue == true) {
                    DecsyncPrefUtils.chooseDecsyncDir(this)
                    return@OnPreferenceChangeListener false
                }
                true
            }
        } else {
            findPreference<Preference>(DECSYNC_ENABLED)?.summary =
                    if (requireContext().getPrefBoolean(DECSYNC_ENABLED, false))
                        requireContext().getPrefString(DECSYNC_FILE, defaultDecsyncDir)
                    else
                        getString(R.string.settings_decsync_enabled_description)
            findPreference<Preference>(DECSYNC_ENABLED)?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
                if (newValue == true) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        chooseDecsyncFile()
                    } else {
                        requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_DECSYNC)
                    }
                    false
                } else {
                    preference.summary = getString(R.string.settings_decsync_enabled_description)
                    true
                }
            }
        }
    }

    override fun onBindPreferences() {
        if (requireActivity().intent.getBooleanExtra(EXTRA_SELECT_SAF_DIR, false)) {
            scrollToPreference(DECSYNC_ENABLED)
            DecsyncPrefUtils.chooseDecsyncDir(this)
        }
    }

    private fun chooseDecsyncFile() {
        val intent = Intent(requireContext(), FilePickerActivity::class.java)
        intent.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true)
        intent.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR)
        // Always start on the default DecSync dir, as the previously selected one may be inaccessible
        intent.putExtra(FilePickerActivity.EXTRA_START_PATH, defaultDecsyncDir)
        startActivityForResult(intent, CHOOSE_DECSYNC_FILE)
    }

    @ExperimentalStdlibApi
    @ObsoleteCoroutinesApi
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requireContext().getPrefBoolean(DECSYNC_USE_SAF, false)) {
            DecsyncPrefUtils.chooseDecsyncDirResult(requireContext(), requestCode, resultCode, data) {
                requireContext().putPrefBoolean(DECSYNC_ENABLED, true)
                findPreference<CheckBoxPreference>(DECSYNC_ENABLED)?.isChecked = true
                if (!requireActivity().intent.getBooleanExtra(EXTRA_SELECT_SAF_DIR, false)) {
                    DecsyncUtils.initSync(requireContext())
                }
            }
        } else {
            if (requestCode == CHOOSE_DECSYNC_FILE) {
                val uri = data?.data
                if (resultCode == Activity.RESULT_OK && uri != null) {
                    requireContext().putPrefBoolean(DECSYNC_ENABLED, true)
                    val dir = Utils.getFileForUri(uri).path
                    requireContext().putPrefString(DECSYNC_FILE, dir)
                    findPreference<CheckBoxPreference>(DECSYNC_ENABLED)?.isChecked = true
                    findPreference<CheckBoxPreference>(DECSYNC_ENABLED)?.summary = dir
                    DecsyncUtils.initSync(requireContext())
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_DECSYNC -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                chooseDecsyncFile()
            }
        }
    }
}