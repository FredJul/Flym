package net.frju.flym.data.entities

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.os.Parcel
import android.os.Parcelable
import paperparcel.PaperParcel

@PaperParcel
@Entity(tableName = "feeds", primaryKeys = arrayOf("id", "link"))
data class Feed(
        var id: String = "",
        var link: String = "",
        @ColumnInfo(name = "feedTitle")
        var title: String? = null,
        var imageLink: String? = null,
        var fetchError: Boolean = false,
        //var favicon: ByteArray? = null
        var retrieveFullText: Boolean = false,
        var isGroup: Boolean = false,
        var groupId: String? = null,
        var displayPriority: Int = 0) : Parcelable {

    companion object {
        @JvmField val CREATOR = PaperParcelFeed.CREATOR

        @JvmField val UNREAD_ITEMS_ID = "unread_items"
        @JvmField val ALL_ITEMS_ID = "all_items"
        @JvmField val FAVORITES_ID = "favorites"
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        PaperParcelFeed.writeToParcel(this, dest, flags)
    }

    fun update(feed: com.einmalfel.earl.Feed) {
        if (title == null) {
            title = feed.title
        }

        if (feed.imageLink != null) {
            imageLink = feed.imageLink
        }
    }
}
