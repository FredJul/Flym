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

package net.frju.flym.data.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import android.arch.persistence.room.Update
import net.frju.flym.data.entities.Task


@Dao
interface TaskDao {
    @get:Query("SELECT * FROM tasks")
    val all: List<Task>

    @get:Query("SELECT * FROM tasks")
    val observeAll: LiveData<List<Task>>

    @get:Query("SELECT * FROM tasks WHERE imageLinkToDl = ''")
    val mobilizeTasks: List<Task>

    @Query("SELECT COUNT(*) FROM tasks WHERE imageLinkToDl = '' AND entryId = :itemId")
    fun observeItemMobilizationTasksCount(itemId: String): LiveData<Int>

    @get:Query("SELECT * FROM tasks WHERE imageLinkToDl != ''")
    val downloadTasks: List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg tasks: Task)

    @Update
    fun update(vararg tasks: Task)

    @Delete
    fun delete(vararg tasks: Task)
}