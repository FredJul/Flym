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
import net.frju.flym.data.entities.DecsyncCategory
import net.frju.flym.data.entities.DecsyncFeed
import net.frju.flym.data.entities.Feed
import net.frju.flym.data.entities.FeedWithCount
import java.util.*

private const val DECSYNC_FEED_SELECT = "feedLink, feedTitle, groupId"
private const val DECSYNC_FEED_WHERE = "isGroup = 0 AND feedLink != ''"
private const val DECSYNC_CATEGORY_SELECT = "feedLink, feedTitle"
private const val DECSYNC_CATEGORY_WHERE = "isGroup = 1 AND feedLink != '' AND feedTitle NOT NULL"
private const val ENTRY_COUNT = "(SELECT COUNT(*) FROM entries WHERE feedId IS f.feedId AND read = 0)"

@Dao
abstract class FeedDao {

    @ExperimentalStdlibApi
    @get:Query("SELECT $DECSYNC_FEED_SELECT FROM feeds WHERE $DECSYNC_FEED_WHERE")
    abstract val observeAllDecsyncFeeds: LiveData<List<DecsyncFeed>>

    @ExperimentalStdlibApi
    @get:Query("SELECT $DECSYNC_CATEGORY_SELECT FROM feeds WHERE $DECSYNC_CATEGORY_WHERE")
    abstract val observeAllDecsyncCategories: LiveData<List<DecsyncCategory>>

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

    @Query("DELETE FROM feeds WHERE feedLink IS :link")
    abstract fun deleteByLink(link: String)

    @Query("UPDATE feeds SET retrieveFullText = 1 WHERE feedId = :feedId")
    abstract fun enableFullTextRetrieval(feedId: Long)

    @Query("UPDATE feeds SET retrieveFullText = 0 WHERE feedId = :feedId")
    abstract fun disableFullTextRetrieval(feedId: Long)

    @Query("UPDATE feeds SET fetchError = 1 WHERE feedId = :feedId")
    abstract fun setFetchError(feedId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(vararg feeds: Feed): List<Long>

    @Update
    abstract fun update(vararg feeds: Feed)

    @Delete
    abstract fun delete(vararg feeds: Feed)
}