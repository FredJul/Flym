package net.frju.flym.data.entities

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.os.Parcel
import android.os.Parcelable
import com.rometools.rome.feed.synd.SyndEntry
import net.frju.flym.App
import net.frju.flym.utils.sha1
import paperparcel.PaperParcel
import java.util.*


@PaperParcel
@Entity(tableName = "entries")
open class Entry : Parcelable {

    @PrimaryKey
    var id: String = ""
    var feedId: Long = 0L
    var link: String? = null
    var fetchDate: Date = Date()
    var publicationDate: Date = fetchDate // important to know if the publication date has been set
    var title: String? = null
    var description: String? = null
    var mobilizedContent: String? = null
    var imageLink: String? = null
    var author: String? = null
    var read: Boolean = false
    var favorite: Boolean = false

    companion object {
        @JvmField
        val CREATOR = PaperParcelEntry.CREATOR
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        PaperParcelEntry.writeToParcel(this, dest, flags)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Entry

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

fun SyndEntry.toDbFormat(feed: Feed): Entry {
    val itemId = (feed.id.toString() + "_" + (uri ?: link ?: title ?: UUID.randomUUID().toString())).sha1()

    val item = App.db.entryDao().findById(itemId) ?: Entry() // TODO possible to not do a db request?
    item.id = itemId
    item.feedId = feed.id
    item.title = title
    item.description = description?.value ?: contents.getOrNull(0)?.value
    item.link = link
    //TODO item.imageLink = null
    item.author = author
    item.publicationDate = if (publishedDate?.before(item.publicationDate) == true) publishedDate!! else item.publicationDate

    return item
}