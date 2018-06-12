package net.frju.flym.data.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import net.frju.flym.data.entities.Feed


@Dao
interface FeedDao {
    @get:Query("SELECT * FROM feeds WHERE isGroup = 0")
    val allNonGroupFeeds: List<Feed>

    @get:Query("SELECT * FROM feeds ORDER BY isGroup DESC, groupId DESC, displayPriority ASC, feedId ASC")
    val all: List<Feed>

    @get:Query("SELECT * FROM feeds ORDER BY isGroup DESC, groupId DESC, displayPriority ASC, feedId ASC")
    val observeAll: LiveData<List<Feed>>

    @Query("SELECT * FROM feeds WHERE feedId IS :id LIMIT 1")
    fun findById(id: Long): Feed?

    @Query("UPDATE feeds SET retrieveFullText = 1 WHERE feedId = :feedId")
    fun enableFullTextRetrieval(feedId: Long)

    @Query("UPDATE feeds SET retrieveFullText = 0 WHERE feedId = :feedId")
    fun disableFullTextRetrieval(feedId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg feeds: Feed)

    @Update
    fun update(vararg feeds: Feed)

    @Delete
    fun delete(vararg feeds: Feed)
}