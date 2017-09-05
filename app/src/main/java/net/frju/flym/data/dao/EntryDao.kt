package net.frju.flym.data.dao

import android.arch.persistence.room.*
import io.reactivex.Flowable
import net.frju.flym.data.entities.Entry
import net.frju.flym.data.entities.EntryWithFeed


@Dao
interface EntryDao {

    @Query("SELECT * FROM entries INNER JOIN feeds ON entries.feedId = feeds.feedId WHERE fetchDate <= :arg0 ORDER BY publicationDate DESC, id")
    fun observeAll(maxDate: Long): Flowable<List<EntryWithFeed>>

    @Query("SELECT * FROM entries INNER JOIN feeds ON entries.feedId = feeds.feedId WHERE entries.feedId IS :arg0 AND fetchDate <= :arg1 ORDER BY publicationDate DESC, id")
    fun observeByFeed(feedId: Long, maxDate: Long): Flowable<List<EntryWithFeed>>

    @Query("SELECT * FROM entries INNER JOIN feeds ON entries.feedId = feeds.feedId WHERE groupId IS :arg0 AND fetchDate <= :arg1 ORDER BY publicationDate DESC, id")
    fun observeByGroup(groupId: Long, maxDate: Long): Flowable<List<EntryWithFeed>>

    @Query("SELECT COUNT(*) FROM entries WHERE read = 0 AND fetchDate > :arg0")
    fun observeNewEntriesCount(minDate: Long): Flowable<Long>

    @Query("SELECT COUNT(*) FROM entries WHERE entries.feedId IS :arg0 AND read = 0 AND fetchDate > :arg1")
    fun observeNewEntriesCountByFeed(feedId: Long, minDate: Long): Flowable<Long>

    @Query("SELECT COUNT(*) FROM entries INNER JOIN feeds ON entries.feedId = feeds.feedId WHERE groupId IS :arg0 AND read = 0 AND fetchDate > :arg1")
    fun observeNewEntriesCountByGroup(groupId: Long, minDate: Long): Flowable<Long>

    @get:Query("SELECT * FROM entries INNER JOIN feeds ON entries.feedId = feeds.feedId WHERE favorite = 1")
    val favorites: List<EntryWithFeed>

    @get:Query("SELECT COUNT(*) FROM entries WHERE read = 0")
    val countUnread: Long

    @Query("SELECT * FROM entries WHERE id IS :arg0 LIMIT 1")
    fun findById(id: String): Entry?

    @Query("SELECT id FROM entries WHERE feedId IS (:arg0)")
    fun checkCurrentIdsForFeed(feedId: Long): List<String>

    @Query("DELETE FROM entries WHERE fetchDate < :arg0 AND favorite = 0")
    fun deleteOlderThan(keepDateBorderTime: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg entries: Entry)

    @Delete
    fun deleteAll(entries: Entry)
}