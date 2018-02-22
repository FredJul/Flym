package net.frju.flym.ui.feeds

import com.bignerdranch.expandablerecyclerview.model.Parent
import net.frju.flym.data.entities.Feed


class FeedGroup(val feed: Feed, val subFeeds: List<Feed>) : Parent<Feed> {

    override fun getChildList(): List<Feed> {
        return subFeeds
    }

    override fun isInitiallyExpanded(): Boolean {
        return false
    }

    // needed to preserve expansion state
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FeedGroup

        if (feed.id != other.feed.id) return false

        return true
    }

    override fun hashCode(): Int {
        return feed.id.hashCode()
    }
}
