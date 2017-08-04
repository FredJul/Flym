package net.frju.flym.ui.items

import android.arch.lifecycle.LifecycleFragment
import android.arch.lifecycle.Observer
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_items.*
import kotlinx.android.synthetic.main.view_main_containers.view.*
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.data.entities.Feed
import net.frju.flym.data.entities.ItemWithFeed
import net.frju.flym.service.FetcherService
import net.frju.flym.ui.main.MainNavigator
import net.frju.parentalcontrol.utils.PrefUtils


class ItemsFragment : LifecycleFragment() {

    private val navigator: MainNavigator by lazy { activity as MainNavigator }

    private var adapter: FlexibleAdapter<ItemWithFeed>? = null
    private var feed: Feed? = null
    private var unfilteredItems: List<ItemWithFeed>? = null

    private val prefListener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (PrefUtils.IS_REFRESHING == key) {
            refreshSwipeProgress()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_items, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        feed = arguments.getParcelable(ARG_FEED)

        setupToolbar()
        setupRecyclerView()
        bottom_navigation.setOnNavigationItemSelectedListener {
            recycler_view.post {
                updateUI()
                recycler_view.scrollToPosition(0)
            }
            true
        }

        if (savedInstanceState != null) {
            //adapter?.selectedItemId = savedInstanceState.getString(STATE_SELECTED_ITEM_ID)
        }

        when {
            feed == null || feed!!.id == Feed.ALL_ITEMS_ID -> App.db.itemDao().observeAll
            feed!!.isGroup -> App.db.itemDao().observeByGroup(feed!!.id)
            else -> App.db.itemDao().observeByFeed(feed!!.id)
        }.observe(this, Observer<List<ItemWithFeed>> {
            unfilteredItems = it
            updateUI()
        })
    }

    private fun updateUI() {
        val items = when (bottom_navigation.selectedItemId) {
            R.id.unreads -> unfilteredItems?.filter { !it.read }
            R.id.favorites -> unfilteredItems?.filter { it.favorite }
            else -> unfilteredItems
        }
        adapter?.updateDataSet(items, false)
    }

    override fun onStart() {
        super.onStart()
        PrefUtils.registerOnPrefChangeListener(prefListener)
        refreshSwipeProgress()
    }

    override fun onStop() {
        super.onStop()
        PrefUtils.unregisterOnPrefChangeListener(prefListener)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        //outState.putString(STATE_SELECTED_ITEM_ID, adapter?.selectedItemId)

        super.onSaveInstanceState(outState)
    }

    private fun setupRecyclerView() {
        recycler_view.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(activity)
        recycler_view.layoutManager = layoutManager
        adapter = FlexibleAdapter<ItemWithFeed>(null, FlexibleAdapter.OnItemClickListener { position ->
            adapter?.getItem(position)?.let { navigator.goToItemDetails(it) }
            true
        }, true)
        recycler_view.adapter = adapter

        refresh_layout.setOnRefreshListener { refreshLayout ->
            context.startService(Intent(context, FetcherService::class.java).setAction(FetcherService.ACTION_REFRESH_FEEDS))
        }
    }

    private fun setupToolbar() {
        val appBar = activity.containers_layout.custom_appbar
        appBar.setTitle(R.string.fragment_people__title)
        appBar.setMenuRes(R.menu.people_general, R.menu.people_specific, R.menu.people_merged)
    }

    fun setSelectedItem(selectedItem: ItemWithFeed) {
        //adapter?.selectedItemId = selectedItem.id
    }

    fun getPreviousItem(): ItemWithFeed? {
        adapter?.selectedPositions?.let {
            if (it.size > 0 && it[0] > 0) {
                return adapter?.getItem(it[0]--)
            }
        }

        return null
    }

    fun getNextItem(): ItemWithFeed? {
        adapter?.selectedPositions?.let {
            if (it.size > 0 && it[0] < adapter!!.currentItems.size - 1) {
                return adapter?.getItem(it[0]++)
            }
        }

        return null
    }

    private fun refreshSwipeProgress() {
        if (PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
            refresh_layout.autoRefresh()
        } else {
            refresh_layout.finishRefresh()
        }
    }

    companion object {

        private val ARG_FEED = "feed"
        private val STATE_SELECTED_ITEM_ID = "state_selected_item_id"

        fun newInstance(feed: Feed?): ItemsFragment {
            val fragment = ItemsFragment()
            val bundle = Bundle()
            bundle.putParcelable(ARG_FEED, feed)
            fragment.arguments = bundle
            return fragment
        }
    }
}
