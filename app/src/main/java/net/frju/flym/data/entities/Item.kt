package net.frju.flym.data.entities

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.os.Parcel
import android.os.Parcelable
import net.frju.flym.App
import paperparcel.PaperParcel
import java.util.*


@PaperParcel
@Entity(tableName = "items")
open class Item : Parcelable {

    @PrimaryKey var id: String = ""
    var feedId: Long = 0L
    var link: String? = null
    var publicationDate: Date = Date()
    var fetchDate: Date = Date()
    var title: String? = null
    var description: String? = null
    var mobilizedContent: String? = null
    var imageLink: String? = null
    var author: String? = null
    var read: Boolean = false
    var favorite: Boolean = false

    companion object {
        @JvmField val CREATOR = PaperParcelItem.CREATOR
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        PaperParcelItem.writeToParcel(this, dest, flags)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Item

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

fun com.einmalfel.earl.Item.toDbFormat(feed: Feed): Item {
    val itemId = feed.id.toString() + "_" + (id ?: link ?: title ?: UUID.randomUUID().toString())

    val item = App.db.itemDao().findById(itemId) ?: Item()
    item.id = itemId
    item.feedId = feed.id
    item.title = title
    item.description = description
    item.link = link
    item.imageLink = imageLink
    item.author = author
    item.publicationDate = publicationDate ?: Date()

    return item
}