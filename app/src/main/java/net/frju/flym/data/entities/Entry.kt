package net.frju.flym.data.entities

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import android.os.Parcelable
import com.rometools.rome.feed.synd.SyndEntry
import kotlinx.android.parcel.Parcelize
import net.frju.flym.utils.sha1
import java.util.Date
import java.util.UUID


@Entity(tableName = "entries",
		indices = [(Index(value = ["feedId"]))],
		foreignKeys = [(ForeignKey(entity = Feed::class,
				parentColumns = ["feedId"],
				childColumns = ["feedId"],
				onDelete = ForeignKey.CASCADE))])
open class Entry {

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

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other?.javaClass != javaClass) return false

		other as Entry

		if (id != other.id) return false

		return true
	}

	override fun hashCode(): Int = id.hashCode()
}

fun SyndEntry.toDbFormat(feed: Feed): Entry {
	val item = Entry()
	item.id = (feed.id.toString() + "_" + (uri ?: link ?: title
	?: UUID.randomUUID().toString())).sha1()
	item.feedId = feed.id
	item.title = title
	item.description = contents.getOrNull(0)?.value ?: description?.value
	item.link = link
	//TODO item.imageLink = null
	item.author = author
	item.publicationDate = if (publishedDate?.before(item.publicationDate) == true) publishedDate!! else item.publicationDate

	return item
}