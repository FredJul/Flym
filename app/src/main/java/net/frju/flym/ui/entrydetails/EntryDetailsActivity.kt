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

package net.frju.flym.ui.entrydetails

import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import net.fred.feedex.R
import net.frju.flym.utils.setupNoActionBarTheme
import org.jetbrains.anko.backgroundColor

class EntryDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setupNoActionBarTheme()

        super.onCreate(savedInstanceState)

        val tv = TypedValue()
        if (theme.resolveAttribute(R.attr.colorPrimary, tv, true)) {
            window.decorView.backgroundColor = tv.data
        }

        if (savedInstanceState == null) {
            val fragment = EntryDetailsFragment().apply {
                arguments = intent.extras
            }

            supportFragmentManager
                    .beginTransaction()
                    .replace(android.R.id.content, fragment)
                    .commitAllowingStateLoss()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }

        return false
    }
}
