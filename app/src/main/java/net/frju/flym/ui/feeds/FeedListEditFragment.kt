package net.frju.flym.ui.feeds

import android.app.AlertDialog
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
import net.frju.flym.ui.views.DragNDropListener
import org.jetbrains.anko.doAsync


class FeedListEditFragment : Fragment() {

	private val feedGroups = mutableListOf<FeedGroup>()
	private val feedAdapter = EditFeedAdapter(feedGroups)

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val view = inflater.inflate(R.layout.fragment_feed_list_edit, container, false)

		view.feedsList.apply {
			layoutManager = LinearLayoutManager(context)
			adapter = feedAdapter

			dragNDropListener = object : DragNDropListener {

				var fromHasGroupIndicator = false

				override fun onStartDrag(itemView: View) {
				}

				override fun onDrag(x: Float, y: Float) {
				}

				override fun onStopDrag(itemView: View) {
				}

				override fun onDrop(posFrom: Int, posTo: Int) {

					val fromIsTopLevel = feedAdapter.getItemViewType(posFrom) == BaseFeedAdapter.TYPE_TOP_LEVEL
					val toIsTopLevel = feedAdapter.getItemViewType(posTo) == BaseFeedAdapter.TYPE_TOP_LEVEL

					val fromFeed = feedAdapter.getFeedAtPos(posFrom)
					val fromIsFeedWithoutGroup = fromIsTopLevel && !fromFeed.isGroup

					val toFeed = feedAdapter.getFeedAtPos(posTo)
					val toIsFeedWithoutGroup = toIsTopLevel && !toFeed.isGroup

//					val packedPosTo = view.feedsList.getExpandableListPosition(flatPosTo)
//					val packedGroupPosTo = getPackedPositionGroup(packedPosTo)

					if ((fromIsFeedWithoutGroup || !fromIsTopLevel) && toIsTopLevel && !toIsFeedWithoutGroup) {
						AlertDialog.Builder(activity) //
								.setTitle(R.string.to_group_title) //
								.setMessage(R.string.to_group_message) //
								.setPositiveButton(R.string.to_group_into) { dialog, which ->
									fromFeed.groupId = toFeed.id
									fromFeed.displayPriority = 1
									doAsync {
										App.db.feedDao().update(fromFeed)
									}
								}.setNegativeButton(R.string.to_group_above) { dialog, which ->
									//moveItem(fromIsGroup, toIsGroup, fromIsFeedWithoutGroup, packedPosTo, packedGroupPosTo, flatPosFrom)
								}.show()
					} else {
						//moveItem(fromIsGroup, toIsGroup, fromIsFeedWithoutGroup, packedPosTo, packedGroupPosTo, flatPosFrom)
					}
				}
			}
		}

		App.db.feedDao().observeAll.observe(this, Observer {
			it?.let {
				feedGroups.clear()

				val subFeedMap = it.groupBy { it.groupId }

				feedGroups.addAll(
						subFeedMap[null]?.map { FeedGroup(it, subFeedMap[it.id].orEmpty()) }.orEmpty()
				)

				feedAdapter.notifyParentDataSetChanged(true)
			}
		})

		return view
	}

//	private fun moveItem(fromIsGroup: Boolean, toIsGroup: Boolean, fromIsFeedWithoutGroup: Boolean, packedPosTo: Long, packedGroupPosTo: Int, flatPosFrom: Int) {
//		if (fromIsGroup && toIsGroup) {
//			values.put(FeedColumns.PRIORITY, packedGroupPosTo + 1)
//			cr.update(FeedColumns.CONTENT_URI(mListView.getItemIdAtPosition(flatPosFrom)), values, null, null)
//		} else if (!fromIsGroup && toIsGroup) {
//			values.put(FeedColumns.PRIORITY, packedGroupPosTo + 1)
//			values.putNull(FeedColumns.GROUP_ID)
//			cr.update(FeedColumns.CONTENT_URI(mListView.getItemIdAtPosition(flatPosFrom)), values, null, null)
//		} else if (!fromIsGroup && !toIsGroup || fromIsFeedWithoutGroup && !toIsGroup) {
//			val groupPrio = ExpandableListView.getPackedPositionChild(packedPosTo) + 1
//			values.put(FeedColumns.PRIORITY, groupPrio)
//
//			val flatGroupPosTo = mListView.getFlatListPosition(ExpandableListView.getPackedPositionForGroup(packedGroupPosTo))
//			values.put(FeedColumns.GROUP_ID, mListView.getItemIdAtPosition(flatGroupPosTo))
//			cr.update(FeedColumns.CONTENT_URI(mListView.getItemIdAtPosition(flatPosFrom)), values, null, null)
//		}
//	}
}
