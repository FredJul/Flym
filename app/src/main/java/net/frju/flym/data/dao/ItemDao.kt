package net.frju.flym.data.dao

import android.arch.persistence.room.*
import io.reactivex.Flowable
import net.frju.flym.data.entities.Item
import net.frju.flym.data.entities.ItemWithFeed


@Dao
interface ItemDao {

    @Query("SELECT * FROM items INNER JOIN feeds ON items.feedId = feeds.feedId WHERE fetchDate <= :arg0 ORDER BY publicationDate DESC, id")
    fun observeAll(maxDate: Long): Flowable<List<ItemWithFeed>>

    @Query("SELECT * FROM items INNER JOIN feeds ON items.feedId = feeds.feedId WHERE items.feedId IS :arg0 AND fetchDate <= :arg1 ORDER BY publicationDate DESC, id")
    fun observeByFeed(feedId: Long, maxDate: Long): Flowable<List<ItemWithFeed>>

    @Query("SELECT * FROM items INNER JOIN feeds ON items.feedId = feeds.feedId WHERE groupId IS :arg0 AND fetchDate <= :arg1 ORDER BY publicationDate DESC, id")
    fun observeByGroup(groupId: Long, maxDate: Long): Flowable<List<ItemWithFeed>>

    @Query("SELECT COUNT(*) FROM items WHERE read = 0 AND fetchDate > :arg0")
    fun observeNewItemsCount(minDate: Long): Flowable<Long>

    @Query("SELECT COUNT(*) FROM items WHERE items.feedId IS :arg0 AND read = 0 AND fetchDate > :arg1")
    fun observeNewItemsCountByFeed(feedId: Long, minDate: Long): Flowable<Long>

    @Query("SELECT COUNT(*) FROM items INNER JOIN feeds ON items.feedId = feeds.feedId WHERE groupId IS :arg0 AND read = 0 AND fetchDate > :arg1")
    fun observeNewItemsCountByGroup(groupId: Long, minDate: Long): Flowable<Long>

    @get:Query("SELECT * FROM items INNER JOIN feeds ON items.feedId = feeds.feedId WHERE favorite = 1")
    val favorites: List<ItemWithFeed>

    @get:Query("SELECT COUNT(*) FROM items WHERE read = 0")
    val countUnread: Long

    @Query("SELECT * FROM items WHERE id IS :arg0 LIMIT 1")
    fun findById(id: String): Item?

    @Query("SELECT id FROM items WHERE feedId IS (:arg0)")
    fun checkCurrentIdsForFeed(feedId: Long): List<String>

    @Query("DELETE FROM items WHERE fetchDate < :arg0 AND favorite = 0")
    fun deleteOlderThan(keepDateBorderTime: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg items: Item)

    @Delete
    fun deleteAll(items: Item)
}