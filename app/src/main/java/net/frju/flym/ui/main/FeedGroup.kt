package net.frju.flym.ui.main

import com.bignerdranch.expandablerecyclerview.model.Parent
import net.frju.flym.data.entities.Feed


class FeedGroup(val feed: Feed, val subFeeds: List<Feed>) : Parent<Feed> {

    override fun getChildList(): List<Feed> {
        return subFeeds
    }

    override fun isInitiallyExpanded(): Boolean {
        return false
    }
}
