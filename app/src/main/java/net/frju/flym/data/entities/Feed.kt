package net.frju.flym.data.entities

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import android.os.Parcel
import android.os.Parcelable
import com.rometools.rome.feed.synd.SyndFeed
import paperparcel.PaperParcel

@PaperParcel
@Entity(tableName = "feeds", indices = arrayOf(Index(value = *arrayOf("feedId", "feedLink"), unique = true)))
data class Feed(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "feedId")
        var id: Long = 0L,
        @ColumnInfo(name = "feedLink")
        var link: String = "",
        @ColumnInfo(name = "feedTitle")
        var title: String? = null,
        @ColumnInfo(name = "feedImageLink")
        var imageLink: String? = null,
        var fetchError: Boolean = false,
        var retrieveFullText: Boolean = false,
        var isGroup: Boolean = false,
        var groupId: Long? = null,
        var displayPriority: Int = 0) : Parcelable {

    companion object {
        @JvmField val CREATOR = PaperParcelFeed.CREATOR

        @JvmField
        val ALL_ENTRIES_ID = -1L
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        PaperParcelFeed.writeToParcel(this, dest, flags)
    }

    fun update(feed: SyndFeed) {
        if (title == null) {
            title = feed.title
        }

        if (feed.image?.url != null) {
            imageLink = feed.image?.url
        }

        // no error anymore since we just got a feed
        fetchError = false
    }
}
