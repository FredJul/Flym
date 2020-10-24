package net.frju.flym.data.entities

import org.decsync.library.items.Rss
import java.util.*

@ExperimentalStdlibApi
data class DecsyncArticle(
        val uri: String,
        val read: Boolean,
        val favorite: Boolean,
        val publicationDate: Date
) {
    fun getRssArticle(): Rss.Article {
        val time = publicationDate.time
        val date = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        date.timeInMillis = time
        val year = date.get(Calendar.YEAR)
        val month = date.get(Calendar.MONTH) + 1
        val day = date.get(Calendar.DAY_OF_MONTH)
        return Rss.Article(uri, read, favorite, year, month, day)
    }
}