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

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import net.frju.flym.App
import net.frju.flym.data.entities.Entry
import net.frju.flym.data.entities.EntryWithFeed
import net.frju.flym.data.utils.PrefConstants.DECSYNC_ENABLED
import net.frju.flym.utils.DecsyncUtils
import net.frju.flym.utils.Extra
import net.frju.flym.utils.getPrefBoolean
import org.decsync.library.Decsync

private const val LIGHT_SELECT = "id, entries.feedId, feedLink, feedTitle, fetchDate, publicationDate, title, link, description, imageLink, read, favorite"
private const val ORDER_BY = "ORDER BY CASE WHEN :isDesc = 1 THEN publicationDate END DESC, CASE WHEN :isDesc = 0 THEN publicationDate END ASC, id"
private const val JOIN = "entries INNER JOIN feeds ON entries.feedId = feeds.feedId"
private const val OLDER = "fetchDate <= :maxDate"
private const val FEED_ID = "entries.feedId IS :feedId"
private const val LIKE_SEARCH = "LIKE '%' || :searchText || '%'"

@Dao
abstract class EntryDao {

    @Query("SELECT $LIGHT_SELECT FROM $JOIN WHERE title $LIKE_SEARCH OR description $LIKE_SEARCH OR mobilizedContent $LIKE_SEARCH $ORDER_BY")
    abstract fun observeSearch(searchText: String, isDesc: Boolean): DataSource.Factory<Int, EntryWithFeed>

    @Query("SELECT $LIGHT_SELECT FROM $JOIN WHERE $OLDER $ORDER_BY")
    abstract fun observeAll(maxDate: Long, isDesc: Boolean): DataSource.Factory<Int, EntryWithFeed>

    @Query("SELECT $LIGHT_SELECT FROM $JOIN WHERE $OLDER AND read = 0 $ORDER_BY")
    abstract fun observeAllUnreads(maxDate: Long, isDesc: Boolean): DataSource.Factory<Int, EntryWithFeed>

    @Query("SELECT $LIGHT_SELECT FROM $JOIN WHERE $OLDER AND favorite = 1 $ORDER_BY")
    abstract fun observeAllFavorites(maxDate: Long, isDesc: Boolean): DataSource.Factory<Int, EntryWithFeed>

    @Query("SELECT id FROM entries WHERE $OLDER $ORDER_BY")
    abstract fun observeAllIds(maxDate: Long, isDesc: Boolean): LiveData<List<String>>

    @Query("SELECT id FROM entries WHERE $OLDER AND read = 0 $ORDER_BY")
    abstract fun observeAllUnreadIds(maxDate: Long, isDesc: Boolean): LiveData<List<String>>

    @Query("SELECT id FROM entries WHERE $OLDER AND favorite = 1 $ORDER_BY")
    abstract fun observeAllFavoriteIds(maxDate: Long, isDesc: Boolean): LiveData<List<String>>

    @Query("SELECT $LIGHT_SELECT FROM $JOIN WHERE $FEED_ID AND $OLDER $ORDER_BY")
    abstract fun observeByFeed(feedId: Long, maxDate: Long, isDesc: Boolean): DataSource.Factory<Int, EntryWithFeed>

    @Query("SELECT $LIGHT_SELECT FROM $JOIN WHERE $FEED_ID AND $OLDER AND read = 0 $ORDER_BY")
    abstract fun observeUnreadsByFeed(feedId: Long, maxDate: Long, isDesc: Boolean): DataSource.Factory<Int, EntryWithFeed>

    @Query("SELECT $LIGHT_SELECT FROM $JOIN WHERE $FEED_ID AND $OLDER AND favorite = 1 $ORDER_BY")
    abstract fun observeFavoritesByFeed(feedId: Long, maxDate: Long, isDesc: Boolean): DataSource.Factory<Int, EntryWithFeed>

    @Query("SELECT id FROM entries WHERE title $LIKE_SEARCH OR description $LIKE_SEARCH OR mobilizedContent $LIKE_SEARCH $ORDER_BY")
    abstract fun observeIdsBySearch(searchText: String, isDesc: Boolean): LiveData<List<String>>

    @Query("SELECT id FROM entries WHERE feedId IS :feedId AND $OLDER $ORDER_BY")
    abstract fun observeIdsByFeed(feedId: Long, maxDate: Long, isDesc: Boolean): LiveData<List<String>>

    @Query("SELECT id FROM entries WHERE feedId IS :feedId AND $OLDER AND read = 0 $ORDER_BY")
    abstract fun observeUnreadIdsByFeed(feedId: Long, maxDate: Long, isDesc: Boolean): LiveData<List<String>>

    @Query("SELECT id FROM entries WHERE feedId IS :feedId AND $OLDER AND favorite = 1 $ORDER_BY")
    abstract fun observeFavoriteIdsByFeed(feedId: Long, maxDate: Long, isDesc: Boolean): LiveData<List<String>>

    @Query("SELECT $LIGHT_SELECT FROM $JOIN WHERE groupId IS :groupId AND $OLDER $ORDER_BY")
    abstract fun observeByGroup(groupId: Long, maxDate: Long, isDesc: Boolean): DataSource.Factory<Int, EntryWithFeed>

    @Query("SELECT $LIGHT_SELECT FROM $JOIN WHERE groupId IS :groupId AND $OLDER AND read = 0 $ORDER_BY")
    abstract fun observeUnreadsByGroup(groupId: Long, maxDate: Long, isDesc: Boolean): DataSource.Factory<Int, EntryWithFeed>

    @Query("SELECT $LIGHT_SELECT FROM $JOIN WHERE groupId IS :groupId AND $OLDER AND favorite = 1 $ORDER_BY")
    abstract fun observeFavoritesByGroup(groupId: Long, maxDate: Long, isDesc: Boolean): DataSource.Factory<Int, EntryWithFeed>

    @Query("SELECT id FROM $JOIN WHERE groupId IS :groupId AND $OLDER $ORDER_BY")
    abstract fun observeIdsByGroup(groupId: Long, maxDate: Long, isDesc: Boolean): LiveData<List<String>>

    @Query("SELECT id FROM $JOIN WHERE groupId IS :groupId AND $OLDER AND read = 0 $ORDER_BY")
    abstract fun observeUnreadIdsByGroup(groupId: Long, maxDate: Long, isDesc: Boolean): LiveData<List<String>>

    @Query("SELECT id FROM $JOIN WHERE groupId IS :groupId AND $OLDER AND favorite = 1 $ORDER_BY")
    abstract fun observeFavoriteIdsByGroup(groupId: Long, maxDate: Long, isDesc: Boolean): LiveData<List<String>>

    @Query("SELECT COUNT(*) FROM entries WHERE read = 0 AND fetchDate > :minDate")
    abstract fun observeNewEntriesCount(minDate: Long): LiveData<Long>

    @Query("SELECT COUNT(*) FROM entries WHERE $FEED_ID AND read = 0 AND fetchDate > :minDate")
    abstract fun observeNewEntriesCountByFeed(feedId: Long, minDate: Long): LiveData<Long>

    @Query("SELECT COUNT(*) FROM $JOIN WHERE groupId IS :groupId AND read = 0 AND fetchDate > :minDate")
    abstract fun observeNewEntriesCountByGroup(groupId: Long, minDate: Long): LiveData<Long>

    @get:Query("SELECT id FROM entries WHERE read = 1")
    abstract val readIds: List<String>

    @get:Query("SELECT id FROM entries WHERE read = 0")
    abstract val unreadIds: List<String>

    @get:Query("SELECT id FROM entries WHERE favorite = 1")
    abstract val favoriteIds: List<String>

    @get:Query("SELECT COUNT(*) FROM entries WHERE read = 0")
    abstract val countUnread: Long

    @Query("SELECT * FROM entries WHERE id IS :id LIMIT 1")
    abstract fun findById(id: String): Entry?

    @Query("SELECT * FROM $JOIN WHERE id IS :id LIMIT 1")
    abstract fun findByIdWithFeed(id: String): EntryWithFeed?

    @Query("SELECT id FROM entries WHERE link IS :link LIMIT 1")
    abstract fun idForLink(link: String): String?

    @Query("SELECT id FROM entries WHERE uri IS :uri LIMIT 1")
    abstract fun idForUri(uri: String): String?

    @Query("SELECT title FROM entries WHERE title IN (:titles)")
    abstract fun findAlreadyExistingTitles(titles: List<String>): List<String>

    @Query("SELECT id FROM entries WHERE feedId IS (:feedId)")
    abstract fun idsForFeed(feedId: Long): List<String>

    @Query("SELECT id FROM entries WHERE feedId IS (:feedId) AND read = 0")
    abstract fun unreadIdsForFeed(feedId: Long): List<String>

    @Query("SELECT id FROM $JOIN WHERE groupId IS (:groupId) AND read = 0")
    abstract fun unreadIdsForGroup(groupId: Long): List<String>

    @Query("UPDATE entries SET read = 1 WHERE id IN (:ids)")
    protected abstract fun markAsReadDao(ids: List<String>)

    @ExperimentalStdlibApi
    fun markAsRead(ids: List<String>, updateDecsync: Boolean = true) {
        if (updateDecsync && App.context.getPrefBoolean(DECSYNC_ENABLED, false)) {
            val entries = ids.mapNotNull { id ->
                getReadMarkEntry(id, "read", true)
            }
            DecsyncUtils.getDecsync(App.context)?.setEntries(entries)
        }
        markAsReadDao(ids)
    }

    @Query("UPDATE entries SET read = 0 WHERE id IN (:ids)")
    protected abstract fun markAsUnreadDao(ids: List<String>)

    @ExperimentalStdlibApi
    fun markAsUnread(ids: List<String>, updateDecsync: Boolean = true) {
        if (updateDecsync && App.context.getPrefBoolean(DECSYNC_ENABLED, false)) {
            val entries = ids.mapNotNull { id ->
                getReadMarkEntry(id, "read", false)
            }
            DecsyncUtils.getDecsync(App.context)?.setEntries(entries)
        }
        markAsUnreadDao(ids)
    }

    @Query("UPDATE entries SET read = 1 WHERE feedId = :feedId")
    protected abstract fun markAsReadDao(feedId: Long)

    @ExperimentalStdlibApi
    fun markAsRead(feedId: Long, updateDecsync: Boolean = true) {
        if (updateDecsync && App.context.getPrefBoolean(DECSYNC_ENABLED, false)) {
            val entries = unreadIdsForFeed(feedId).mapNotNull { id ->
                getReadMarkEntry(id, "read", true)
            }
            DecsyncUtils.getDecsync(App.context)?.setEntries(entries)
        }
        markAsReadDao(feedId)
    }

    @Query("UPDATE entries SET read = 1 WHERE feedId IN (SELECT feedId FROM feeds WHERE groupId = :groupId)")
    protected abstract fun markGroupAsReadDao(groupId: Long)

    @ExperimentalStdlibApi
    fun markGroupAsRead(groupId: Long, updateDecsync: Boolean = true) {
        if (updateDecsync && App.context.getPrefBoolean(DECSYNC_ENABLED, false)) {
            val entries = unreadIdsForGroup(groupId).mapNotNull { id ->
                getReadMarkEntry(id, "read", true)
            }
            DecsyncUtils.getDecsync(App.context)?.setEntries(entries)
        }
        markGroupAsReadDao(groupId)
    }

    @Query("UPDATE entries SET read = 1")
    protected abstract fun markAllAsReadDao()

    @ExperimentalStdlibApi
    fun markAllAsRead(updateDecsync: Boolean = true) {
        if (updateDecsync && App.context.getPrefBoolean(DECSYNC_ENABLED, false)) {
            val entries = unreadIds.mapNotNull { id ->
                getReadMarkEntry(id, "read", true)
            }
            DecsyncUtils.getDecsync(App.context)?.setEntries(entries)
        }
        markAllAsReadDao()
    }

    @Query("UPDATE entries SET favorite = 1 WHERE id IS :id")
    protected abstract fun markAsFavoriteDao(id: String)

    @ExperimentalStdlibApi
    fun markAsFavorite(id: String, updateDecsync: Boolean = true) {
        if (updateDecsync && App.context.getPrefBoolean(DECSYNC_ENABLED, false)) {
            writeReadMarkEntry(id, "marked", true)
        }
        markAsFavoriteDao(id)
    }

    @Query("UPDATE entries SET favorite = 0 WHERE id IS :id")
    protected abstract fun markAsNotFavoriteDao(id: String)

    @ExperimentalStdlibApi
    fun markAsNotFavorite(id: String, updateDecsync: Boolean = true) {
        if (updateDecsync && App.context.getPrefBoolean(DECSYNC_ENABLED, false)) {
            writeReadMarkEntry(id, "marked", false)
        }
        markAsNotFavoriteDao(id)
    }

    @ExperimentalStdlibApi
    fun getReadMarkEntry(id: String, type: String, value: Boolean): Decsync.EntryWithPath? {
        val entry = findById(id) ?: return null
        return entry.getDecsyncEntry(type, value)
    }

    @ExperimentalStdlibApi
    private fun writeReadMarkEntry(id: String, type: String, value: Boolean) {
        val entry = getReadMarkEntry(id, type, value) ?: return
        DecsyncUtils.getDecsync(App.context)?.setEntries(listOf(entry))
    }

    @Query("DELETE FROM entries WHERE fetchDate < :keepDateBorderTime AND favorite = 0 AND read = :read")
    abstract fun deleteOlderThan(keepDateBorderTime: Long, read: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE) // Ignore because we don't want to delete previously starred entries
    protected abstract fun insertDao(vararg entries: Entry)

    @ExperimentalStdlibApi
    fun insert(entries: List<Entry>) {
        insertDao(*entries.toTypedArray())
        val storedEntries = mutableListOf<Decsync.StoredEntry>()
        for (entry in entries) {
            storedEntries.add(entry.getDecsyncStoredEntry("read") ?: continue)
            storedEntries.add(entry.getDecsyncStoredEntry("marked") ?: continue)
        }
        val extra = Extra()
        DecsyncUtils.getDecsync(App.context)?.executeStoredEntries(storedEntries, extra)
    }

    @Update
    abstract fun update(vararg entries: Entry)

    @Delete
    abstract fun delete(vararg entries: Entry)
}