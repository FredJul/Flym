package net.frju.flym.data.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import net.frju.flym.data.entities.Feed


@Dao
interface FeedDao {
    @get:Query("SELECT * FROM feeds")
    val all: List<Feed>

    @get:Query("SELECT * FROM feeds WHERE isGroup=1 OR groupId IS NULL ORDER BY displayPriority ASC, feedId ASC")
    val observeRootItems: LiveData<List<Feed>>

    @Query("SELECT * FROM feeds WHERE feedId IS :arg0 LIMIT 1")
    fun findById(id: Long): Feed?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg feeds: Feed)

    @Delete
    fun deleteAll(feeds: Feed)
}