package net.frju.flym.data.entities

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import android.os.Parcelable
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.rometools.rome.feed.synd.SyndFeed
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "feeds",
		indices = [(Index(value = ["groupId"])), (Index(value = ["feedId", "feedLink"], unique = true))],
		foreignKeys = [(ForeignKey(entity = Feed::class,
				parentColumns = ["feedId"],
				childColumns = ["groupId"],
				onDelete = ForeignKey.CASCADE))])
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

		const val ALL_ENTRIES_ID = -1L

		fun getLetterDrawable(feedId: Long, feedTitle: String?, rounded: Boolean = false): TextDrawable {
			val feedName = feedTitle.orEmpty()

			val color = ColorGenerator.DEFAULT.getColor(feedId) // The color is specific to the feedId (which shouldn't change)
			val lettersForName = if (feedName.length < 2) feedName.toUpperCase() else feedName.substring(0, 2).toUpperCase()
			return if (rounded) {
				TextDrawable.builder().buildRound(lettersForName, color)
			} else {
				TextDrawable.builder().buildRect(lettersForName, color)
			}
		}
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

	fun getLetterDrawable(rounded: Boolean = false): TextDrawable {
		return getLetterDrawable(id, title, rounded)
	}
}
