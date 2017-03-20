package net.frju.flym.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.thoughtbot.expandablerecyclerview.ExpandableRecyclerViewAdapter
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup
import com.thoughtbot.expandablerecyclerview.viewholders.ChildViewHolder
import com.thoughtbot.expandablerecyclerview.viewholders.GroupViewHolder
import kotlinx.android.synthetic.main.view_feed.view.*
import net.fred.feedex.R
import net.frju.flym.data.Feed
import org.jetbrains.anko.onClick

private val STATE_SELECTED_ID = "STATE_SELECTED_ID"

class FeedAdapter(groups: List<FeedGroup>) : ExpandableRecyclerViewAdapter<FeedAdapter.FeedGroupViewHolder, FeedAdapter.FeedViewHolder>(groups) {

    var feedClickListener: ((View, Feed) -> Unit)? = null
    var selectedItemId: String? = null
        set(newValue) {
            notifyDataSetChanged()
            field = newValue
        }

    fun onFeedClick(listener: (View, Feed) -> Unit) {
        feedClickListener = listener
    }

    override fun onCreateGroupViewHolder(parent: ViewGroup, viewType: Int): FeedGroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_feed, parent, false)
        return FeedGroupViewHolder(view)
    }

    override fun onCreateChildViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_feed, parent, false)
        return FeedViewHolder(view)
    }

    override fun onBindChildViewHolder(holder: FeedViewHolder, flatPosition: Int, group: ExpandableGroup<*>,
                                       childIndex: Int) {
        holder.bindItem((group as FeedGroup).items[childIndex])
    }

    override fun onBindGroupViewHolder(holder: FeedGroupViewHolder, flatPosition: Int,
                                       group: ExpandableGroup<*>) {
        holder.bindItem(group as FeedGroup)
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle?) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState?.putString(STATE_SELECTED_ID, selectedItemId)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        selectedItemId = savedInstanceState?.getString(STATE_SELECTED_ID)
    }

    override fun getItemCount(): Int {
        // Needed to avoid a crash with the lib when there is 0 item
        try {
            return super.getItemCount()
        } catch (e: Exception) {
        }

        return 0
    }

    inner class FeedGroupViewHolder(itemView: View)
        : GroupViewHolder(itemView) {

        fun bindItem(group: FeedGroup) {
            itemView.isSelected = selectedItemId == group.feed.id
            itemView.title.text = group.title
            itemView.onClick {
                if (!group.feed.isGroup) {
                    selectedItemId = group.feed.id
                    feedClickListener?.invoke(itemView, group.feed)
                } else {
                    toggleGroup(group)
                }
            }
        }
    }

    inner class FeedViewHolder(itemView: View)
        : ChildViewHolder(itemView) {

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