package net.frju.flym.ui.main

import android.annotation.SuppressLint
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup

import net.frju.flym.data.Feed


@SuppressLint("ParcelCreator")
class FeedGroup(val feed: Feed, items: List<Feed>) : ExpandableGroup<Feed>(feed.title, items)
