package net.frju.flym.data.entities

import net.frju.flym.App
import org.decsync.library.items.Rss

@ExperimentalStdlibApi
data class DecsyncFeed(
        val feedLink: String,
        val feedTitle: String?,
        val groupId: Long?
) {
    fun getRssFeed(): Rss.Feed {
        return Rss.Feed(feedLink, feedTitle, groupId) {
            groupId?.let { App.db.feedDao().findById(it)?.link }
        }
    }
}

@ExperimentalStdlibApi
data class DecsyncCategory(
        val feedLink: String,
        val feedTitle: String
) {
    fun getRssCategory() : Rss.Category {
        return Rss.Category(feedLink, feedTitle, null) {
            // We do not support nested categories
            // Only changes are detected, so always giving the default value of null is fine
            null
        }
    }
}