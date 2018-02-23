package net.frju.flym.ui.feeds

import net.fred.feedex.R

class EditFeedAdapter(groups: List<FeedGroup>) : BaseFeedAdapter(groups) {
	override val layoutId = R.layout.view_feed_edit
}