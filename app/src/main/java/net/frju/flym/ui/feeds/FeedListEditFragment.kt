package net.frju.flym.ui.feeds

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_feed_list_edit.view.*
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.ui.main.FeedAdapter
import net.frju.flym.ui.main.FeedGroup

class FeedListEditFragment : Fragment() {

	private val feedGroups = mutableListOf<FeedGroup>()
	private val feedAdapter = FeedAdapter(feedGroups)

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val view = inflater.inflate(R.layout.fragment_feed_list_edit, container, false)

		view.feedsList.layoutManager = LinearLayoutManager(context)
		view.feedsList.adapter = feedAdapter

		App.db.feedDao().observeAll.observe(this, Observer {
			it?.let {
				feedGroups.clear()

				val subFeedMap = it.groupBy { it.groupId }

				feedGroups.addAll(
						subFeedMap[null]?.map { FeedGroup(it, subFeedMap[it.id].orEmpty()) }.orEmpty()
				)

				feedAdapter.notifyParentDataSetChanged(true)

//				feedAdapter.onFeedClick { view, feed ->
//					goToEntriesList(feed)
//					closeDrawer()
//				}
//
//				feedAdapter.onFeedLongClick { view, feed ->
//					if (feed.isGroup) {
//						return@onFeedLongClick
//					}
//
//					PopupMenu(this, view).apply {
//						setOnMenuItemClickListener { item ->
//							when (item.itemId) {
//								R.id.mark_all_as_read -> doAsync { App.db.entryDao().markAsRead(feed.id) }
//								R.id.rename -> {
//								}
//								R.id.enable_full_text_retrieval -> {
//								}
//								R.id.disable_full_text_retrieval -> {
//								}//TODO
//							}
//							true
//						}
//						inflate(R.menu.drawer_feed)
//
//						show()
//					}
//				}
			}
		})

		return view
	}
}
