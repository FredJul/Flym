/*
 * Copyright (c) 2018 CodingSpiderFox
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

package net.frju.flym.data.tasks

import android.os.AsyncTask
import net.frju.flym.data.entities.Filter
import net.frju.flym.App
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import kotlin.collections.ArrayList

class InsertFiltersTask() : AsyncTask<ArrayList<String>, Void, Void>() {

    override fun doInBackground(vararg params: ArrayList<String>?): Void? {
        var filters = ArrayList<Filter>()

        params.forEach { param ->
            if(param != null) {
                param.forEach { value ->
                    if (value != null && !value.isEmpty() && !value.isEmpty()) {
                        var filter = Filter()
                        filter.keywordToIgnore = value
                        filter.dateCreated = LocalDateTime.now()
                        filters.add(filter)
                    }
                }
            }
        }
        App.db.filterDao().insert(*filters.toTypedArray())
        return null
    }
}