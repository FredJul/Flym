/*
 * Copyright (c) 2012-2018 Frederic Julian
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package net.frju.flym.data.entities

import android.content.Context
import android.os.Parcelable
import android.text.Html
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rometools.rome.feed.synd.SyndEntry
import kotlinx.android.parcel.Parcelize
import net.fred.feedex.R
import net.frju.flym.utils.sha1
import java.util.*


@Parcelize
@Entity(tableName = "entries",
        indices = [(Index(value = ["feedId"])), (Index(value = ["link"], unique = true))],
        foreignKeys = [(ForeignKey(entity = Feed::class,
                parentColumns = ["feedId"],
                childColumns = ["feedId"],
                onDelete = ForeignKey.CASCADE))])
data class Entry(@PrimaryKey
                 var id: String = "",
                 var feedId: Long = 0L,
                 var link: String? = null,
                 var fetchDate: Date = Date(),
                 var publicationDate: Date = fetchDate, // important to know if the publication date has been set
                 var title: String? = null,
                 var description: String? = null,
                 var mobilizedContent: String? = null,
                 var imageLink: String? = null,
                 var author: String? = null,
                 var read: Boolean = false,
                 var favorite: Boolean = false) : Parcelable {

    fun getReadablePublicationDate(context: Context): String =
            if (DateUtils.isToday(publicationDate.time)) {
                DateFormat.getTimeFormat(context).format(publicationDate)
            } else {
                DateFormat.getMediumDateFormat(context).format(publicationDate) + ' ' +
                        DateFormat.getTimeFormat(context).format(publicationDate)
            }

    override fun equals(other: Any?): Boolean {
        other?.let { o ->
            if (this == o) {
                return true
            }else if(this.javaClass != o.javaClass){
                return false
            }else{
                val otherEntry = o as Entry
                return this.id == otherEntry.id || this.link == otherEntry.link
            }
        }
        return super.equals(other)
    }
}

fun SyndEntry.toDbFormat(context: Context, feed: Feed): Entry {
    val item = Entry()
    item.id = (feed.id.toString() + "_" + (link ?: uri ?: title
    ?: UUID.randomUUID().toString())).sha1()
    item.feedId = feed.id
    @Suppress("DEPRECATION")
    if (title != null) {
        item.title = Html.fromHtml(title).toString()
    } else {
        item.title = context.getString(R.string.entry_default_title)
    }
    item.description = contents.getOrNull(0)?.value ?: description?.value
    item.link = link
    //TODO item.imageLink = null
    item.author = author

    val date = publishedDate ?: updatedDate
    item.publicationDate = if (date?.before(item.publicationDate) == true) date else item.publicationDate

    return item
}