package net.frju.flym.ui.main

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

class FeedAdapter(groups: List<FeedGroup>) : ExpandableRecyclerViewAdapter<FeedAdapter.FeedGroupViewHolder, FeedAdapter.FeedViewHolder>(groups) {

    var feedClickListener: ((Feed) -> Unit)? = null

    fun onFeedClick(listener: (Feed) -> Unit) {
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
            itemView.title.text = group.title
            itemView.onClick {
                if (!group.feed.isGroup) {
                    feedClickListener?.invoke(group.feed)
                } else {
                    toggleGroup(group)
                }
            }
        }
    }

    inner class FeedViewHolder(itemView: View)
        : ChildViewHolder(itemView) {

        fun bindItem(feed: Feed) {
            itemView.title.text = feed.title
            itemView.onClick {
                feedClickListener?.invoke(feed)
            }
        }
    }
}