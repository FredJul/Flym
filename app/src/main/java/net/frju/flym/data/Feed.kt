package net.frju.flym.data

import android.os.Parcel
import android.os.Parcelable
import net.frju.androidquery.annotation.DbField
import net.frju.androidquery.annotation.DbModel
import net.frju.androidquery.annotation.InitMethod
import net.frju.androidquery.database.ModelListener
import net.frju.androidquery.gen.FEED
import net.frju.androidquery.operation.condition.Where
import net.frju.flym.data.db.LocalDatabaseProvider
import paperparcel.PaperParcel
import java.util.*

@PaperParcel
@DbModel(databaseProvider = LocalDatabaseProvider::class)
class Feed : Parcelable, ModelListener {

    @DbField(primaryKey = true)
    var id = ""

    @DbField(unique = true)
    var link = ""

    @DbField
    var title: String? = null

    @DbField
    var imageLink: String? = null

    @DbField
    var fetchError = false

    @DbField
    var favicon: ByteArray? = null

    @DbField
    var retrieveFullText = false

    @DbField
    var isGroup = false

    @DbField
    var groupId: String? = null

    @DbField
    var displayPriority = 0

    var subFeeds: List<Feed>? = null

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        PaperParcelFeed.writeToParcel(this, dest, flags)
    }

    override fun onPreInsert() {
        if (id.isEmpty()) {
            id = UUID.randomUUID().toString()
        }
    }

    override fun onPreUpdate() {
    }

    override fun onPreDelete() {
    }

    @InitMethod
    fun initSubFeeds() {
        if (isGroup) {
            subFeeds = FEED.select().where(Where.field(FEED.GROUP_ID).isEqualTo(id)).orderByDesc(FEED.DISPLAY_PRIORITY).query().toList()
        }
    }

    fun update(feed: com.einmalfel.earl.Feed) {
        if (title == null) {
            title = feed.title
        }

        if (feed.imageLink != null) {
            imageLink = feed.imageLink
        }
    }

    companion object {
        @JvmField val CREATOR = PaperParcelFeed.CREATOR

        @JvmField val UNREAD_ITEMS_ID = "unread_items"
        @JvmField val ALL_ITEMS_ID = "all_items"
        @JvmField val FAVORITES_ID = "favorites"
    }
}