package net.frju.flym.data.dao

import android.arch.lifecycle.LiveData
import android.arch.paging.LivePagedListProvider
import android.arch.persistence.room.*
import net.frju.flym.data.entities.Entry
import net.frju.flym.data.entities.EntryWithFeed


@Dao
interface EntryDao {

    @Query("SELECT * FROM entries INNER JOIN feeds ON entries.feedId = feeds.feedId WHERE fetchDate <= :maxDate ORDER BY publicationDate DESC, id")
    fun observeAll(maxDate: Long): LivePagedListProvider<Int, EntryWithFeed>

    @Query("SELECT * FROM entries INNER JOIN feeds ON entries.feedId = feeds.feedId WHERE fetchDate <= :maxDate AND read = 0 ORDER BY publicationDate DESC, id")
    fun observeAllUnreads(maxDate: Long): LivePagedListProvider<Int, EntryWithFeed>

    @Query("SELECT * FROM entries INNER JOIN feeds ON entries.feedId = feeds.feedId WHERE fetchDate <= :maxDate AND favorite = 1 ORDER BY publicationDate DESC, id")
    fun observeAllFavorites(maxDate: Long): LivePagedListProvider<Int, EntryWithFeed>

    @Query("SELECT id FROM entries WHERE fetchDate <= :maxDate ORDER BY publicationDate DESC, id")
    fun observeAllIds(maxDate: Long): LiveData<List<String>>

    @Query("SELECT id FROM entries WHERE fetchDate <= :maxDate AND read = 0 ORDER BY publicationDate DESC, id")
    fun observeAllUnreadIds(maxDate: Long): LiveData<List<String>>

    @Query("SELECT id FROM entries WHERE fetchDate <= :maxDate AND favorite = 1 ORDER BY publicationDate DESC, id")
    fun observeAllFavoriteIds(maxDate: Long): LiveData<List<String>>

    @Query("SELECT * FROM entries INNER JOIN feeds ON entries.feedId = feeds.feedId WHERE entries.feedId IS :feedId AND fetchDate <= :maxDate ORDER BY publicationDate DESC, id")
    fun observeByFeed(feedId: Long, maxDate: Long): LivePagedListProvider<Int, EntryWithFeed>

    @Query("SELECT * FROM entries INNER JOIN feeds ON entries.feedId = feeds.feedId WHERE entries.feedId IS :feedId AND fetchDate <= :maxDate AND read = 0 ORDER BY publicationDate DESC, id")
    fun observeUnreadsByFeed(feedId: Long, maxDate: Long): LivePagedListProvider<Int, EntryWithFeed>

    @Query("SELECT * FROM entries INNER JOIN feeds ON entries.feedId = feeds.feedId WHERE entries.feedId IS :feedId AND fetchDate <= :maxDate AND favorite = 1 ORDER BY publicationDate DESC, id")
    fun observeFavoritesByFeed(feedId: Long, maxDate: Long): LivePagedListProvider<Int, EntryWithFeed>

    @Query("SELECT id FROM entries WHERE feedId IS :feedId AND fetchDate <= :maxDate ORDER BY publicationDate DESC, id")
    fun observeIdsByFeed(feedId: Long, maxDate: Long): LiveData<List<String>>

    @Query("SELECT id FROM entries WHERE feedId IS :feedId AND fetchDate <= :maxDate AND read = 0 ORDER BY publicationDate DESC, id")
    fun observeUnreadIdsByFeed(feedId: Long, maxDate: Long): LiveData<List<String>>

    @Query("SELECT id FROM entries WHERE feedId IS :feedId AND fetchDate <= :maxDate AND favorite = 1 ORDER BY publicationDate DESC, id")
    fun observeFavoriteIdsByFeed(feedId: Long, maxDate: Long): LiveData<List<String>>

    @Query("SELECT * FROM entries INNER JOIN feeds ON entries.feedId = feeds.feedId WHERE groupId IS :groupId AND fetchDate <= :maxDate ORDER BY publicationDate DESC, id")
    fun observeByGroup(groupId: Long, maxDate: Long): LivePagedListProvider<Int, EntryWithFeed>

    @Query("SELECT * FROM entries INNER JOIN feeds ON entries.feedId = feeds.feedId WHERE groupId IS :groupId AND fetchDate <= :maxDate AND read = 0 ORDER BY publicationDate DESC, id")
    fun observeUnreadsByGroup(groupId: Long, maxDate: Long): LivePagedListProvider<Int, EntryWithFeed>

    @Query("SELECT * FROM entries INNER JOIN feeds ON entries.feedId = feeds.feedId WHERE groupId IS :groupId AND fetchDate <= :maxDate AND favorite = 1 ORDER BY publicationDate DESC, id")
    fun observeFavoritesByGroup(groupId: Long, maxDate: Long): LivePagedListProvider<Int, EntryWithFeed>

    @Query("SELECT id FROM entries INNER JOIN feeds ON entries.feedId = feeds.feedId WHERE groupId IS :groupId AND fetchDate <= :maxDate ORDER BY publicationDate DESC, id")
    fun observeIdsByGroup(groupId: Long, maxDate: Long): LiveData<List<String>>

    @Query("SELECT id FROM entries INNER JOIN feeds ON entries.feedId = feeds.feedId WHERE groupId IS :groupId AND fetchDate <= :maxDate AND read = 0 ORDER BY publicationDate DESC, id")
    fun observeUnreadIdsByGroup(groupId: Long, maxDate: Long): LiveData<List<String>>

    @Query("SELECT id FROM entries INNER JOIN feeds ON entries.feedId = feeds.feedId WHERE groupId IS :groupId AND fetchDate <= :maxDate AND favorite = 1 ORDER BY publicationDate DESC, id")
    fun observeFavoriteIdsByGroup(groupId: Long, maxDate: Long): LiveData<List<String>>

    @Query("SELECT COUNT(*) FROM entries WHERE read = 0 AND fetchDate > :minDate")
    fun observeNewEntriesCount(minDate: Long): LiveData<Long>

    @Query("SELECT COUNT(*) FROM entries WHERE entries.feedId IS :feedId AND read = 0 AND fetchDate > :minDate")
    fun observeNewEntriesCountByFeed(feedId: Long, minDate: Long): LiveData<Long>

    @Query("SELECT COUNT(*) FROM entries INNER JOIN feeds ON entries.feedId = feeds.feedId WHERE groupId IS :groupId AND read = 0 AND fetchDate > :minDate")
    fun observeNewEntriesCountByGroup(groupId: Long, minDate: Long): LiveData<Long>

    @get:Query("SELECT * FROM entries INNER JOIN feeds ON entries.feedId = feeds.feedId WHERE favorite = 1")
    val favorites: List<EntryWithFeed>

    @get:Query("SELECT COUNT(*) FROM entries WHERE read = 0")
    val countUnread: Long

    @Query("SELECT * FROM entries WHERE id IS :id LIMIT 1")
    fun findById(id: String): Entry?

    @Query("SELECT * FROM entries INNER JOIN feeds ON entries.feedId = feeds.feedId WHERE id IS :id LIMIT 1")
    fun findByIdWithFeed(id: String): EntryWithFeed?

    @Query("SELECT id FROM entries WHERE feedId IS (:feedId)")
    fun idsForFeed(feedId: Long): List<String>

    @Query("UPDATE entries SET read = 1 WHERE id IN (:ids)")
    fun markAsRead(ids: List<String>)

    @Query("UPDATE entries SET read = 0 WHERE id IN (:ids)")
    fun markAsUnread(ids: List<String>)

    @Query("DELETE FROM entries WHERE fetchDate < :keepDateBorderTime AND favorite = 0 AND read = 0")
    fun deleteOlderThan(keepDateBorderTime: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg entries: Entry)

    @Delete
    fun deleteAll(entries: Entry)
}