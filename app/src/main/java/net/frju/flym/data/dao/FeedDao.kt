package net.frju.flym.data.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import net.frju.flym.data.entities.Feed


@Dao
interface FeedDao {
    @get:Query("SELECT * FROM feeds")
    val all: List<Feed>

    @get:Query("SELECT * FROM feeds ORDER BY isGroup DESC, groupId DESC, displayPriority ASC, feedId ASC")
    val observeAll: LiveData<List<Feed>>

    @Query("SELECT * FROM feeds WHERE feedId IS :id LIMIT 1")
    fun findById(id: Long): Feed?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg feeds: Feed)

    @Delete
    fun delete(vararg feeds: Feed)
}