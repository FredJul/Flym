package net.frju.flym.data

import android.os.Parcel
import android.os.Parcelable
import net.frju.androidquery.annotation.DbField
import net.frju.androidquery.annotation.DbModel
import net.frju.androidquery.annotation.InitMethod
import net.frju.androidquery.gen.FEED
import net.frju.androidquery.operation.condition.Where
import net.frju.flym.data.db.LocalDatabaseProvider
import paperparcel.PaperParcel

@PaperParcel
@DbModel(databaseProvider = LocalDatabaseProvider::class)
class Feed : Parcelable {

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

    @InitMethod
    fun initSubFeeds() {
        if (groupId != null) {
            subFeeds = FEED.select().where(Where.field(FEED.GROUP_ID).isEqualTo(groupId)).orderByDesc(FEED.DISPLAY_PRIORITY).query().toList()
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
    }
}