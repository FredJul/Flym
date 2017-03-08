package net.frju.flym.data

import android.content.ContentValues
import android.os.Parcel
import android.os.Parcelable
import net.frju.androidquery.annotation.DbField
import net.frju.androidquery.annotation.DbModel
import net.frju.androidquery.converter.DateConverter
import net.frju.androidquery.gen.ITEM
import net.frju.flym.data.db.LocalDatabaseProvider
import paperparcel.PaperParcel
import java.util.*

@PaperParcel
@DbModel(databaseProvider = LocalDatabaseProvider::class)
class Item : Parcelable {

    @DbField(primaryKey = true)
    var id = ""

    @DbField
    var feedId = ""

    @DbField
    var link: String? = null

    @DbField
    var publicationDate = Date()

    @DbField
    var fetchDate = Date()

    @DbField
    var title: String? = null

    @DbField
    var description: String? = null

    @DbField
    var mobilizedContent: String? = null

    @DbField
    var imageLink: String? = null

    @DbField
    var author: String? = null

    @DbField
    var read = false

    @DbField
    var favorite = false

    @DbField
    var feed: Feed? = null // The target model for a potential join

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        PaperParcelItem.writeToParcel(this, dest, flags)
    }

    companion object {
        @JvmField val CREATOR = PaperParcelItem.CREATOR
    }
}

fun com.einmalfel.earl.Item.toUpdateValues(): ContentValues {
    val values = ContentValues()

    values.put(ITEM.AUTHOR, author)
    values.put(ITEM.TITLE, title)
    values.put(ITEM.DESCRIPTION, description)
    values.put(ITEM.LINK, link)
    values.put(ITEM.IMAGE_LINK, imageLink)
    values.put(ITEM.PUBLICATION_DATE, DateConverter().convertToDb(publicationDate))

    return values
}

fun com.einmalfel.earl.Item.toDbFormat(feed: Feed): Item {
    val item = Item()
    item.id = feed.id + "_" + (id ?: link ?: title ?: UUID.randomUUID().toString())
    item.feedId = feed.id
    item.title = title
    item.description = description
    item.link = link
    item.imageLink = imageLink
    item.author = author
    item.publicationDate = publicationDate ?: Date()

    return item
}