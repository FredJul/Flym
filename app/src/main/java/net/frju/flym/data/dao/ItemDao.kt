package net.frju.flym.data.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import net.frju.flym.data.entities.Item
import net.frju.flym.data.entities.ItemWithFeed


@Dao
interface ItemDao {
    @get:Query("SELECT * FROM items")
    val all: List<Item>

    @get:Query("SELECT * FROM items INNER JOIN feeds ON items.feedId = feeds.id ORDER BY items.publicationDate DESC, id")
    val observeAll: LiveData<List<ItemWithFeed>>

    @get:Query("SELECT * FROM items INNER JOIN feeds ON items.feedId = feeds.id WHERE favorite = 1")
    val favorites: List<ItemWithFeed>

    @Query("SELECT * FROM items INNER JOIN feeds ON items.feedId = feeds.id WHERE feedId IS :arg0")
    fun observeByFeed(feedId: String): LiveData<List<ItemWithFeed>>

    @Query("SELECT * FROM items INNER JOIN feeds ON items.feedId = feeds.id WHERE groupId IS :arg0")
    fun observeByGroup(groupId: String): LiveData<List<ItemWithFeed>>

    @get:Query("SELECT COUNT(*) FROM items WHERE read = 0")
    val countUnread: Int

    @Query("SELECT * FROM items WHERE id IS :arg0 LIMIT 1")
    fun findById(id: String): Item?

    @Query("SELECT * FROM items WHERE id IN (:arg0)")
    fun findByIds(ids: List<String>): List<Item>

    @Query("DELETE FROM items WHERE fetchDate < :arg0 AND favorite = 0")
    fun deleteOlderThan(keepDateBorderTime: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg items: Item)

    @Delete
    fun deleteAll(items: Item)
}