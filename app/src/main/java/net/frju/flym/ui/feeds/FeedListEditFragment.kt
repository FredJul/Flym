/*
 * Copyright (c) 2012-2018 Frederic Julian
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package net.frju.flym.ui.feeds

import android.app.AlertDialog
import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import android.widget.EditText
import kotlinx.android.synthetic.main.fragment_feed_list_edit.view.*
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.data.entities.Feed
import net.frju.flym.ui.views.DragNDropListener
import org.jetbrains.anko.doAsync
import java.util.*


class FeedListEditFragment : Fragment() {

    private val feedGroups = mutableListOf<FeedGroup>()
    private val feedAdapter = EditFeedAdapter(feedGroups)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_feed_list_edit, container, false)

        view.feedsList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = feedAdapter

            dragNDropListener = object : DragNDropListener {

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
                    val fromIsFeedWithoutGroup = fromIsTopLevel && !fromFeed.feed.isGroup

                    val toFeed = feedAdapter.getFeedAtPos(posTo)
                    val toIsFeedWithoutGroup = toIsTopLevel && !toFeed.feed.isGroup

                    if ((fromIsFeedWithoutGroup || !fromIsTopLevel) && toIsTopLevel && !toIsFeedWithoutGroup) {
                        AlertDialog.Builder(activity)
                                .setTitle(R.string.to_group_title)
                                .setMessage(R.string.to_group_message)
                                .setPositiveButton(R.string.to_group_into) { dialog, which ->
                                    fromFeed.feed.groupId = toFeed.feed.id
                                    changeItemPriority(fromFeed.feed, 1) // TODO would be better at the end instead of beginning
                                }.setNegativeButton(R.string.to_group_above) { dialog, which ->
                                    fromFeed.feed.groupId = toFeed.feed.groupId
                                    changeItemPriority(fromFeed.feed, toFeed.feed.displayPriority)
                                }.show()
                    } else {
                        fromFeed.feed.groupId = toFeed.feed.groupId
                        changeItemPriority(fromFeed.feed, toFeed.feed.displayPriority)
                    }
                }
            }
        }

        App.db.feedDao().observeAllWithCount.observe(this, Observer {
            it?.let {
                feedGroups.clear()

                val subFeedMap = it.groupBy { it.feed.groupId }

                feedGroups.addAll(
                        subFeedMap[null]?.map { FeedGroup(it, subFeedMap[it.feed.id].orEmpty()) }.orEmpty()
                )

                feedAdapter.notifyParentDataSetChanged(true)
            }
        })

        setHasOptionsMenu(true)

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

		inflater.inflate(R.menu.menu_fragment_feed_list_edit, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.add_group -> {
                val input = EditText(activity).apply {
                    setSingleLine(true)
                }

                AlertDialog.Builder(activity)
                        .setTitle(R.string.add_group_title)
                        .setView(input)
                        .setPositiveButton(android.R.string.ok) { dialog, which ->
                            val groupName = input.text.toString()
                            if (groupName.isNotBlank()) {
                                doAsync {
                                    val newGroup = Feed().apply {
                                        title = groupName
                                        isGroup = true
                                    }
                                    App.db.feedDao().insert(newGroup)
                                }
                            }
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                return true
            }
        }

        return false
    }

    private fun changeItemPriority(fromFeed: Feed, newDisplayPriority: Int) {
        fromFeed.lastManualActionUid = UUID.randomUUID().toString() // Needed hack to avoid recursive trigger
        fromFeed.displayPriority = newDisplayPriority

        doAsync {
            App.db.feedDao().update(fromFeed)
        }
    }
}
