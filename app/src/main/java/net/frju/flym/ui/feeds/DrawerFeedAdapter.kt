package net.frju.flym.ui.feeds

import android.os.Bundle
import android.view.View
import net.frju.flym.data.entities.Feed


private const val STATE_SELECTED_ID = "STATE_SELECTED_ID"

open class FeedAdapter(groups: List<FeedGroup>) : BaseFeedAdapter(groups) {

	var selectedItemId = Feed.ALL_ENTRIES_ID
		set(newValue) {
			notifyParentDataSetChanged(true)
			field = newValue
		}

	override fun bindItem(itemView: View, feed: Feed) {
		itemView.isSelected = selectedItemId == feed.id

		itemView.setOnClickListener {
			selectedItemId = feed.id
			feedClickListener?.invoke(itemView, feed)
		}
	}

	override fun bindItem(itemView: View, group: FeedGroup) {
		itemView.isSelected = selectedItemId == group.feed.id

		itemView.setOnClickListener {
			selectedItemId = group.feed.id
			feedClickListener?.invoke(itemView, group.feed)
		}
	}

	override fun onSaveInstanceState(savedInstanceState: Bundle) {
		savedInstanceState.putLong(STATE_SELECTED_ID, selectedItemId)
	}

	override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
		selectedItemId = savedInstanceState?.getLong(STATE_SELECTED_ID) ?: Feed.ALL_ENTRIES_ID
	}
}