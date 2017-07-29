package net.frju.flym.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bignerdranch.expandablerecyclerview.ChildViewHolder
import com.bignerdranch.expandablerecyclerview.ExpandableRecyclerAdapter
import com.bignerdranch.expandablerecyclerview.ParentViewHolder
import kotlinx.android.synthetic.main.view_feed.view.*
import net.fred.feedex.R
import net.frju.flym.data.entities.Feed
import org.jetbrains.anko.sdk21.coroutines.onClick


private val STATE_SELECTED_ID = "STATE_SELECTED_ID"

class FeedAdapter(groups: List<FeedGroup>) : ExpandableRecyclerAdapter<FeedGroup, Feed, FeedAdapter.FeedGroupViewHolder, FeedAdapter.FeedViewHolder>(groups) {

    var feedClickListener: ((View, Feed) -> Unit)? = null
    var selectedItemId: String = Feed.UNREAD_ITEMS_ID
        set(newValue) {
            notifyParentDataSetChanged(true)
            field = newValue
        }

    fun onFeedClick(listener: (View, Feed) -> Unit) {
        feedClickListener = listener
    }

    override fun onCreateParentViewHolder(parentViewGroup: ViewGroup, viewType: Int): FeedGroupViewHolder {
        val view = LayoutInflater.from(parentViewGroup.context).inflate(R.layout.view_feed, parentViewGroup, false)
        return FeedGroupViewHolder(view)
    }

    override fun onCreateChildViewHolder(childViewGroup: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(childViewGroup.context).inflate(R.layout.view_feed, childViewGroup, false)
        return FeedViewHolder(view)
    }

    override fun onBindParentViewHolder(groupViewHolder: FeedGroupViewHolder, parentPosition: Int, group: FeedGroup) {
        groupViewHolder.bindItem(group)
    }

    override fun onBindChildViewHolder(feedViewHolder: FeedViewHolder, parentPosition: Int, childPosition: Int, feed: Feed) {
        feedViewHolder.bindItem(feed)
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putString(STATE_SELECTED_ID, selectedItemId)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        selectedItemId = savedInstanceState?.getString(STATE_SELECTED_ID) ?: Feed.UNREAD_ITEMS_ID
    }

    inner class FeedGroupViewHolder(itemView: View)
        : ParentViewHolder<FeedGroup, Feed>(itemView) {

        fun bindItem(group: FeedGroup) {
            if (group.feed.isGroup) {
                itemView.icon.setImageResource(R.drawable.ic_keyboard_arrow_down_white_24dp)
                itemView.icon.onClick {
                    if (isExpanded) {
                        itemView.icon.animate().rotationBy(-180F).start()
                        collapseView()
                    } else {
                        itemView.icon.animate().rotationBy(180F).start()
                        expandView()
                    }
                }
            }
            itemView.isSelected = selectedItemId == group.feed.id
            itemView.title.text = group.feed.title
            itemView.onClick {
                selectedItemId = group.feed.id
                feedClickListener?.invoke(itemView, group.feed)
            }
        }

        override fun shouldItemViewClickToggleExpansion(): Boolean {
            return false
        }
    }

    inner class FeedViewHolder(itemView: View)
        : ChildViewHolder<Feed>(itemView) {

        fun bindItem(feed: Feed) {
            itemView.isSelected = selectedItemId == feed.id
            itemView.title.text = feed.title
            itemView.onClick {
                selectedItemId = feed.id
                feedClickListener?.invoke(itemView, feed)
            }
        }
    }
}