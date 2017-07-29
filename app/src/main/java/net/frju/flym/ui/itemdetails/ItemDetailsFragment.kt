package net.frju.flym.ui.itemdetails

import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_item_details.*
import me.thanel.swipeactionview.SwipeActionView
import me.thanel.swipeactionview.SwipeGestureListener
import net.fred.feedex.R
import net.frju.flym.data.entities.Item
import net.frju.flym.ui.main.MainNavigator
import org.jetbrains.annotations.NotNull


class ItemDetailsFragment : Fragment() {

    private val navigator: MainNavigator by lazy { activity as MainNavigator }

    private var item: Item? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_item_details, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        item = arguments.getParcelable(ARG_ITEM)

        setupToolbar()

        swipe_view.swipeGestureListener = object : SwipeGestureListener {
            override fun onSwipedLeft(@NotNull swipeActionView: SwipeActionView): Boolean {
                navigator.goToNextItem()
                return true
            }

            override fun onSwipedRight(@NotNull swipeActionView: SwipeActionView): Boolean {
                navigator.goToPreviousItem()
                return true
            }
        }

        updateUI()
    }

    private fun updateUI() {

        if (item != null) {
            collapsing_toolbar.title = item!!.title ?: ""
            title.text = item!!.title ?: ""

            val dateStringBuilder = StringBuilder(DateFormat.getLongDateFormat(context).format(item!!.publicationDate)).append(' ').append(
                    DateFormat.getTimeFormat(context).format(item!!.publicationDate))

            if (item!!.author != null && item!!.author!!.isNotEmpty()) {
                dateStringBuilder.append(" — ").append(item!!.author)
            }

            subtitle.text = dateStringBuilder.toString()
        } else {
            collapsing_toolbar.title = ""
            title.text = ""
            subtitle.text = ""
        }

        item_view.setItem(item)
    }

    private fun setupToolbar() {
        toolbar.inflateMenu(R.menu.person_details)

        if (!activity.containers_layout.hasTwoColumns()) {
            toolbar.setNavigationIcon(R.drawable.ic_back_24dp)
            toolbar.setNavigationOnClickListener { activity.onBackPressed() }
        }
    }

    fun setItem(item: Item) {
        this.item = item
        arguments.putParcelable(ARG_ITEM, item)

        updateUI()
    }

    companion object {

        private val ARG_ITEM = "item"

        fun newInstance(item: Item): ItemDetailsFragment {
            val fragment = ItemDetailsFragment()
            val bundle = Bundle()
            bundle.putParcelable(ARG_ITEM, item)
            fragment.arguments = bundle
            return fragment
        }
    }
}
