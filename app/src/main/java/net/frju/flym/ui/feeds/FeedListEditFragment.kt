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
import net.frju.flym.ui.views.DragNDropListener

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
//					fromHasGroupIndicator = itemView.indicator.getVisibility() == View.VISIBLE
                }

                override fun onDrag(x: Float, y: Float) {
                }

                override fun onStopDrag(itemView: View) {
                }

                override fun onDrop(flatPosFrom: Int, flatPosTo: Int) {
//					val fromIsGroup = ExpandableListView.getPackedPositionType(mListView.getExpandableListPosition(flatPosFrom)) == ExpandableListView.PACKED_POSITION_TYPE_GROUP
//					val toIsGroup = ExpandableListView.getPackedPositionType(mListView.getExpandableListPosition(flatPosTo)) == ExpandableListView.PACKED_POSITION_TYPE_GROUP
//
//					val fromIsFeedWithoutGroup = fromIsGroup && !fromHasGroupIndicator
//
//					val toView = mListView.getChildAt(flatPosTo - mListView.getFirstVisiblePosition())
//					val toIsFeedWithoutGroup = toIsGroup && toView.findViewById(R.id.indicator).getVisibility() != View.VISIBLE
//
//					val packedPosTo = mListView.getExpandableListPosition(flatPosTo)
//					val packedGroupPosTo = ExpandableListView.getPackedPositionGroup(packedPosTo)
//
//					if ((fromIsFeedWithoutGroup || !fromIsGroup) && toIsGroup && !toIsFeedWithoutGroup) {
//						AlertDialog.Builder(activity) //
//								.setTitle(R.string.to_group_title) //
//								.setMessage(R.string.to_group_message) //
//								.setPositiveButton(R.string.to_group_into) { dialog, which ->
//									val values = ContentValues()
//									values.put(FeedColumns.PRIORITY, 1)
//									values.put(FeedColumns.GROUP_ID, mListView.getItemIdAtPosition(flatPosTo))
//
//									val cr = activity!!.contentResolver
//									cr.update(FeedColumns.CONTENT_URI(mListView.getItemIdAtPosition(flatPosFrom)), values, null, null)
//									cr.notifyChange(FeedColumns.GROUPS_CONTENT_URI, null)
//								}.setNegativeButton(R.string.to_group_above) { dialog, which -> moveItem(fromIsGroup, toIsGroup, fromIsFeedWithoutGroup, packedPosTo, packedGroupPosTo, flatPosFrom) }.show()
//					} else {
//						moveItem(fromIsGroup, toIsGroup, fromIsFeedWithoutGroup, packedPosTo, packedGroupPosTo, flatPosFrom)
//					}
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
