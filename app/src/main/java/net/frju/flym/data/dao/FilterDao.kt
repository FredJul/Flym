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

package net.frju.flym.data.dao

import androidx.room.*
import androidx.lifecycle.LiveData
import net.frju.flym.data.entities.Filter


@Dao
abstract class FilterDao {
    @get:Query("SELECT * FROM filters ORDER BY keywordToIgnore ASC")
    abstract val all: List<Filter>

    @get:Query("SELECT * FROM filters")
    abstract val observeAll: LiveData<List<Filter>>

    @Query("SELECT * FROM filters WHERE keywordToIgnore IS :keywordToIgnore LIMIT 1")
    abstract fun findByKeywordToIgnore(keywordToIgnore: String): Filter

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(vararg filters: Filter)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(filter: Filter)

    @Update
    abstract fun update(vararg tasks: Filter)

    @Delete
    abstract fun delete(vararg tasks: Filter)

    @Query("DELETE FROM filters")
    abstract fun deleteAll()
}