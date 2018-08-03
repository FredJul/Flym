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
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import net.fred.feedex.R
import net.frju.flym.data.utils.PrefUtils
import net.frju.flym.data.tasks.DeleteAllFiltersTask
import net.frju.flym.data.tasks.InsertFiltersTask

class SettingsActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.activity_settings)

		supportActionBar?.setDisplayHomeAsUpEnabled(true)
	}

	override fun onBackPressed() {
		super.onBackPressed()
		updateFilters()
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			android.R.id.home -> {
				finish()
			}
		}
		return true
	}

	override fun onDestroy() {
		super.onDestroy()
		updateFilters()
	}

	fun updateFilters() {
		var deleteAllTask = DeleteAllFiltersTask()
		deleteAllTask.execute()

		var filterString = PrefUtils.getString(PrefUtils.FILTER_KEYWORDS, "")

		if(filterString != null && !filterString.isNullOrEmpty() && !filterString.isBlank()) {
			var filterStrings = filterString.split("\n")
			var finalFilters = ArrayList<String>()

			if(filterStrings != null) {
				for (currentFilterString in filterStrings) {
					var finalFilterString = currentFilterString.trim()
					if(finalFilterString != null
							&& !finalFilterString.isBlank()
							&& !finalFilterString.isEmpty()) {
						finalFilters.add(finalFilterString)
					}
				}
			}

			var insertAllTask = InsertFiltersTask()
			insertAllTask.execute(finalFilters)
		}
	}
}