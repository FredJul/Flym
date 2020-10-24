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

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.serialization.json.JsonPrimitive
import net.frju.flym.App
import net.frju.flym.data.entities.Feed
import net.frju.flym.data.entities.FeedWithCount
import net.frju.flym.data.utils.PrefConstants.DECSYNC_ENABLED
import net.frju.flym.utils.DecsyncUtils
import net.frju.flym.utils.Extra
import net.frju.flym.utils.getPrefBoolean
import org.decsync.library.Decsync
import java.util.*

private const val ENTRY_COUNT = "(SELECT COUNT(*) FROM entries WHERE feedId IS f.feedId AND read = 0)"
private const val TAG = "FeedDao"

@Dao
abstract class FeedDao {
    @get:Query("SELECT * FROM feeds WHERE isGroup = 0")
    abstract val allNonGroupFeeds: List<Feed>

    @Query("SELECT * FROM feeds WHERE isGroup = 0 and groupId = :groupId")
    abstract fun allFeedsInGroup(groupId: Long): List<Feed>

    @get:Query("SELECT * FROM feeds ORDER BY groupId DESC, displayPriority ASC, feedId ASC")
    abstract val all: List<Feed>

    @get:Query("SELECT * FROM feeds ORDER BY groupId DESC, displayPriority ASC, feedId ASC")
    abstract val observeAll: LiveData<List<Feed>>

    @get:Query("SELECT *, $ENTRY_COUNT AS entryCount FROM feeds AS f ORDER BY groupId DESC, displayPriority ASC, feedId ASC")
    abstract val observeAllWithCount: LiveData<List<FeedWithCount>>

    @Query("SELECT * FROM feeds WHERE feedId IS :id LIMIT 1")
    abstract fun findById(id: Long): Feed?

    @Query("SELECT * FROM feeds WHERE feedLink IS :link")
    abstract fun findByLink(link: String): Feed?

    @Query("UPDATE feeds SET retrieveFullText = 1 WHERE feedId = :feedId")
    abstract fun enableFullTextRetrieval(feedId: Long)

    @Query("UPDATE feeds SET retrieveFullText = 0 WHERE feedId = :feedId")
    abstract fun disableFullTextRetrieval(feedId: Long)

    @Query("UPDATE feeds SET fetchError = 1 WHERE feedId = :feedId")
    abstract fun setFetchError(feedId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun insertDao(vararg feeds: Feed): List<Long>

    @ExperimentalStdlibApi
    fun insert(feeds: List<Feed>, updateDecsync: Boolean = true): List<Long> {
        for (feed in feeds) {
            if (feed.isGroup && feed.link.isEmpty()) {
                feed.link = "catID%05".format(Random().nextInt(100000))
            }
        }
        if (updateDecsync && App.context.getPrefBoolean(DECSYNC_ENABLED, false)) {
            val entries = mutableListOf<Decsync.EntryWithPath>()
            for (feed in feeds) {
                if (feed.isGroup) {
                    entries.add(Decsync.EntryWithPath(listOf("categories", "names"), JsonPrimitive(feed.link), JsonPrimitive(feed.title)))
                } else {
                    entries.add(Decsync.EntryWithPath(listOf("feeds", "subscriptions"), JsonPrimitive(feed.link), JsonPrimitive(true)))
                    if (feed.title != null) {
                        entries.add(Decsync.EntryWithPath(listOf("feeds", "names"), JsonPrimitive(feed.link), JsonPrimitive(feed.title)))
                    }
                    feed.groupId?.let { findById(it) }?.let { group ->
                        entries.add(Decsync.EntryWithPath(listOf("feeds", "categories"), JsonPrimitive(feed.link), JsonPrimitive(group.link)))
                    }
                }
            }
            DecsyncUtils.getDecsync(App.context)?.setEntries(entries)
        }
        val ids = insertDao(*feeds.toTypedArray())
        if (App.context.getPrefBoolean(DECSYNC_ENABLED, false)) {
            for (feed in feeds) {
                val extra = Extra()
                if (feed.isGroup) {
                    DecsyncUtils.getDecsync(App.context)?.executeStoredEntry(listOf("categories", "names"), JsonPrimitive(feed.link), extra)
                } else {
                    DecsyncUtils.getDecsync(App.context)?.executeStoredEntry(listOf("feeds", "names"), JsonPrimitive(feed.link), extra)
                    DecsyncUtils.getDecsync(App.context)?.executeStoredEntry(listOf("feeds", "categories"), JsonPrimitive(feed.link), extra)
                }
            }
        }
        return ids
    }

    @ExperimentalStdlibApi
    fun insert(feed: Feed, updateDecsync: Boolean = true): Long {
        val ids = insert(listOf(feed), updateDecsync)
        if (ids.size != 1) {
            Log.w(TAG, "Wrong insertion for feed $feed")
            return 0
        }
        return ids[0]
    }

    @Update
    protected abstract fun updateDao(vararg feeds: Feed)

    @ExperimentalStdlibApi
    fun update(feeds: List<Feed>, updateDecsync: Boolean = true) {
        if (updateDecsync && App.context.getPrefBoolean(DECSYNC_ENABLED, false)) {
            val entries = mutableListOf<Decsync.EntryWithPath>()
            for (feed in feeds) {
                val origFeed = findById(feed.id) ?: continue
                if (feed.isGroup) {
                    if (origFeed.title != feed.title) {
                        entries.add(Decsync.EntryWithPath(listOf("categories", "names"), JsonPrimitive(feed.link), JsonPrimitive(feed.title)))
                    }
                } else {
                    var newLink = false
                    if (origFeed.link != feed.link) {
                        entries.add(Decsync.EntryWithPath(listOf("feeds", "subscriptions"), JsonPrimitive(origFeed.link), JsonPrimitive(false)))
                        entries.add(Decsync.EntryWithPath(listOf("feeds", "subscriptions"), JsonPrimitive(feed.link), JsonPrimitive(true)))
                        newLink = true
                    }
                    if (newLink || origFeed.title != feed.title) {
                        entries.add(Decsync.EntryWithPath(listOf("feeds", "names"), JsonPrimitive(feed.link), JsonPrimitive(feed.title)))
                    }
                    if (newLink || origFeed.groupId != feed.groupId) {
                        val group = feed.groupId?.let { findById(it) }
                        entries.add(Decsync.EntryWithPath(listOf("feeds", "categories"), JsonPrimitive(feed.link), JsonPrimitive(group?.link)))
                    }
                }
            }
            DecsyncUtils.getDecsync(App.context)?.setEntries(entries)
        }
        updateDao(*feeds.toTypedArray())
    }

    @ExperimentalStdlibApi
    fun update(feed: Feed, updateDecsync: Boolean = true) = update(listOf(feed), updateDecsync)

    @Delete
    protected abstract fun deleteDao(vararg feeds: Feed)

    @ExperimentalStdlibApi
    fun delete(feeds: List<Feed>, updateDecsync: Boolean = true) {
        if (updateDecsync && App.context.getPrefBoolean(DECSYNC_ENABLED, false)) {
            val entries = feeds.mapNotNull { feed ->
                if (feed.isGroup) return@mapNotNull null
                Decsync.Entry(JsonPrimitive(feed.link), JsonPrimitive(false))
            }
            DecsyncUtils.getDecsync(App.context)?.setEntriesForPath(listOf("feeds", "subscriptions"), entries)
        }
        deleteDao(*feeds.toTypedArray())
    }

    @ExperimentalStdlibApi
    fun delete(feed: Feed, updateDecsync: Boolean = true) = delete(listOf(feed), updateDecsync)

    @ExperimentalStdlibApi
    fun deleteById(id: Long, updateDecsync: Boolean = true) {
        val feed = findById(id) ?: return
        delete(feed, updateDecsync)
    }

    @ExperimentalStdlibApi
    fun deleteByLink(link: String, updateDecsync: Boolean = true) {
        val feed = findByLink(link) ?: return
        delete(feed, updateDecsync)
    }
}