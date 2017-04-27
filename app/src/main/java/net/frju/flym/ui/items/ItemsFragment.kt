package net.frju.flym.ui.items

import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_items.*
import kotlinx.android.synthetic.main.view_main_containers.view.*
import net.fred.feedex.R
import net.frju.androidquery.operation.function.CursorResult
import net.frju.flym.data.Feed
import net.frju.flym.data.Item
import net.frju.flym.service.FetcherService
import net.frju.flym.ui.main.MainActivity
import net.frju.flym.ui.main.MainNavigator
import net.frju.parentalcontrol.utils.PrefUtils


class ItemsFragment : SwipeRefreshFragment() {

    private val navigator: MainNavigator by lazy { activity as MainNavigator }

    private var adapter: ItemsAdapter? = null
    private var feed: Feed? = null

    private val USER_LOADER_ID = 0

    private val prefListener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (PrefUtils.IS_REFRESHING == key) {
            refreshSwipeProgress()
        }
    }

    private val loaderCallbacks = object : LoaderManager.LoaderCallbacks<CursorResult<Item>> {

        override fun onCreateLoader(id: Int, args: Bundle?): Loader<CursorResult<Item>> {
            val loader = ItemsLoader(context, feed)
            loader.setUpdateThrottle(100)
            return loader
        }

        override fun onLoadFinished(loader: Loader<CursorResult<Item>>, data: CursorResult<Item>) {
            adapter?.setItems(data)
            recycler_view.scrollToPosition(0)
        }

        override fun onLoaderReset(loader: Loader<CursorResult<Item>>) {
        }
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_items, container, true)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        feed = arguments.getParcelable(ARG_FEED)

        setupToolbar()
        setupRecyclerView()

        if (savedInstanceState != null) {
            adapter?.selectedItemId = savedInstanceState.getString(STATE_SELECTED_ITEM_ID)
        }

        loaderManager.initLoader(USER_LOADER_ID, null, loaderCallbacks)
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
        outState.putString(STATE_SELECTED_ITEM_ID, adapter?.selectedItemId)

        super.onSaveInstanceState(outState)
    }

    private fun setupRecyclerView() {
        recycler_view.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(activity)
        recycler_view.layoutManager = layoutManager
        adapter = ItemsAdapter()
        adapter?.setOnItemClickListener(object : ItemAdapterView.OnItemClickListener {
            override fun onItemClick(item: Item) {
                navigator.goToItemDetails(item)
            }

            override fun onItemFavoriteIconClick(item: Item) {
                // toast?
            }
        })
        recycler_view.adapter = adapter
    }

    private fun setupToolbar() {
        val appBar = (activity as MainActivity).containers_layout.custom_appbar
        appBar.setTitle(R.string.fragment_people__title)
        appBar.setMenuRes(R.menu.people_general, R.menu.people_specific, R.menu.people_merged)
    }

    override fun onRefresh() {
        context.startService(Intent(context, FetcherService::class.java).setAction(FetcherService.ACTION_REFRESH_FEEDS))
    }

    fun setSelectedItem(selectedItem: Item) {
        adapter?.selectedItemId = selectedItem.id
    }

    fun getNextItem(): Item? {
        return adapter?.getNextItem()
    }

    fun getPreviousItem(): Item? {
        return adapter?.getPreviousItem()
    }

    private fun refreshSwipeProgress() {
        if (PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
            showSwipeProgress()
        } else {
            hideSwipeProgress()
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
