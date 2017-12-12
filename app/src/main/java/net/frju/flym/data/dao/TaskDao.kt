package net.frju.flym.data.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
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

    @Delete
    fun delete(vararg tasks: Task)
}