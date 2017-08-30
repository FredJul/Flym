package net.frju.flym.ui.items

import android.arch.lifecycle.LifecycleFragment
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_items.*
import kotlinx.android.synthetic.main.view_main_containers.view.*
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.GlideApp
import net.frju.flym.data.entities.Feed
import net.frju.flym.data.entities.ItemWithFeed
import net.frju.flym.service.FetcherService
import net.frju.flym.ui.main.MainNavigator
import net.frju.flym.utils.indefiniteSnackbar
import net.frju.flym.utils.loadFavicon
import net.frju.parentalcontrol.utils.PrefUtils
import net.idik.lib.slimadapter.viewinjector.IViewInjector
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.sdk21.coroutines.onClick
import java.util.*
import java.util.concurrent.TimeUnit


class ItemsFragment : LifecycleFragment() {

    private val navigator: MainNavigator by lazy { activity as MainNavigator }

    private var adapter: ItemAdapter? = null
    private var feed: Feed? = null
    private var unfilteredItems: List<ItemWithFeed>? = null
    private var listDisplayDate = Date().time
    private val disposables = CompositeDisposable()
    private var newItemsSnackbar: Snackbar? = null

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

        if (arguments?.containsKey(ARG_FEED) == true) {
            feed = arguments.getParcelable(ARG_FEED)
        }

        setupToolbar()
        setupRecyclerView()

        bottom_navigation.setOnNavigationItemSelectedListener {
            recycler_view.post {
                updateUI()
                recycler_view.scrollToPosition(0)
            }
            true
        }

        read_all_fab.onClick {
            unfilteredItems?.let {
                doAsync {
                    App.db.itemDao().markAsRead(it.map { it.id })
                }
            }
        }

        if (savedInstanceState != null) {
            adapter?.selectedItemId = savedInstanceState.getString(STATE_SELECTED_ITEM_ID)
            listDisplayDate = savedInstanceState.getLong(STATE_LIST_DISPLAY_DATE)
        }

        initDataObservers()
    }

    private fun initDataObservers() {
        disposables.clear()

        val itemsFlow = when {
            feed == null || feed!!.id == Feed.ALL_ITEMS_ID -> App.db.itemDao().observeAll(listDisplayDate)
            feed!!.isGroup -> App.db.itemDao().observeByGroup(feed!!.id, listDisplayDate)
            else -> App.db.itemDao().observeByFeed(feed!!.id, listDisplayDate)
        }

        disposables.add(
                itemsFlow.subscribeOn(Schedulers.io())
                        .debounce(500, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            unfilteredItems = it
                            updateUI()
                        })

        disposables.add(App.db.itemDao().observeNewItemsCount(listDisplayDate)
                .subscribeOn(Schedulers.io())
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it > 0) {
                        if (newItemsSnackbar?.isShown != true) {
                            newItemsSnackbar = coordinator.indefiniteSnackbar("$it new items", "refresh") {
                                listDisplayDate = Date().time
                                initDataObservers()
                            }
                        } else {
                            newItemsSnackbar?.setText("$it new items")
                        }
                    }
                })
    }

    override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

    private fun updateUI() {
        empty_view_refresh_button.visibility = when (bottom_navigation.selectedItemId) {
            R.id.favorites -> View.GONE
            else -> View.VISIBLE
        }

        val items = when (bottom_navigation.selectedItemId) {
            R.id.unreads -> unfilteredItems?.filter { !it.read }
            R.id.favorites -> unfilteredItems?.filter { it.favorite }
            else -> unfilteredItems
        }
        adapter?.updateData(items)
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
        outState.putLong(STATE_LIST_DISPLAY_DATE, listDisplayDate)

        super.onSaveInstanceState(outState)
    }

    private fun setupRecyclerView() {
        recycler_view.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(activity)
        recycler_view.layoutManager = layoutManager
        adapter = ItemAdapter().apply {
            register<ItemWithFeed>(R.layout.view_item) { item, injector ->
                injector
                        .clicked(R.id.item_container) {
                            navigator.goToItemDetails(item)
                        }
                        .clicked(R.id.favorite_icon) { view ->
                            item.favorite = !item.favorite

                            (view as? ImageView)?.let {
                                if (item.favorite) {
                                    it.setImageResource(R.drawable.ic_star_white_24dp)
                                } else {
                                    it.setImageResource(R.drawable.ic_star_border_white_24dp)
                                }
                            }

                            doAsync {
                                App.db.itemDao().insertAll(item)
                            }
                        }
                        .with(R.id.title, IViewInjector.Action<TextView> { view ->
                            view.isEnabled = !item.read
                            view.text = item.title
                        })
                        .with(R.id.feed_name, IViewInjector.Action<TextView> { view ->
                            view.isEnabled = !item.read
                            view.text = item.feedTitle ?: ""
                        })
                        .with(R.id.main_icon, IViewInjector.Action<ImageView> { view ->
                            val feedName = item.feedTitle ?: ""

                            val mainImgUrl = if (TextUtils.isEmpty(item.imageLink)) null else FetcherService.getDownloadedOrDistantImageUrl(item.id, item.imageLink!!)

                            val color = ColorGenerator.DEFAULT.getColor(item.feedId) // The color is specific to the feedId (which shouldn't change)
                            val lettersForName = if (feedName.length < 2) feedName.toUpperCase() else feedName.substring(0, 2).toUpperCase()
                            val letterDrawable = TextDrawable.builder().buildRect(lettersForName, color)
                            if (mainImgUrl != null) {
                                GlideApp.with(view.context).load(mainImgUrl).centerCrop().placeholder(letterDrawable).error(letterDrawable).into(view)
                            } else {
                                GlideApp.with(view.context).clear(view)
                                view.setImageDrawable(letterDrawable)
                            }
                        })
                        .with(R.id.feed_icon, IViewInjector.Action<ImageView> { view ->
                            view.loadFavicon(item.feedLink)
                        })
                        .with(R.id.favorite_icon, IViewInjector.Action<ImageView> { view ->
                            if (item.favorite) {
                                view.setImageResource(R.drawable.ic_star_white_24dp)
                            } else {
                                view.setImageResource(R.drawable.ic_star_border_white_24dp)
                            }
                        })
            }

            attachTo(recycler_view)
        }

        refresh_layout.setOnRefreshListener { refreshLayout ->
            startRefresh()
        }

        recycler_view.emptyView = empty_view

        empty_view_refresh_button.onClick {
            startRefresh()
        }
    }

    private fun startRefresh() {
        // TODO specify the feed to refresh
        context.startService(Intent(context, FetcherService::class.java).setAction(FetcherService.ACTION_REFRESH_FEEDS))
    }

    private fun setupToolbar() {
        val appBar = activity.containers_layout.custom_appbar
        appBar.setTitle(R.string.fragment_people__title)
        appBar.setMenuRes(R.menu.people_general, R.menu.people_specific, R.menu.people_merged)
    }

    fun setSelectedItem(selectedItem: ItemWithFeed) {
        adapter?.selectedItemId = selectedItem.id
    }

    fun getPreviousItem(): ItemWithFeed? {
        return adapter?.previousItem
    }

    fun getNextItem(): ItemWithFeed? {
        return adapter?.nextItem
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
        private val STATE_SELECTED_ITEM_ID = "STATE_SELECTED_ITEM_ID"
        private val STATE_LIST_DISPLAY_DATE = "STATE_LIST_DISPLAY_DATE"

        fun newInstance(feed: Feed?): ItemsFragment {
            val fragment = ItemsFragment()
            feed?.let {
                fragment.arguments = bundleOf(ARG_FEED to feed)
            }
            return fragment
        }
    }
}
