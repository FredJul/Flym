package net.frju.flym.ui.entrydetails

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_entry_details.*
import me.thanel.swipeactionview.SwipeActionView
import me.thanel.swipeactionview.SwipeGestureListener
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.data.entities.EntryWithFeed
import net.frju.flym.ui.main.MainNavigator
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.doAsync
import org.jetbrains.annotations.NotNull


class EntryDetailsFragment : Fragment() {

    private val navigator: MainNavigator by lazy { activity as MainNavigator }

    private var entry: EntryWithFeed? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_entry_details, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        entry = arguments.getParcelable(ARG_ENTRY)

        setupToolbar()

        swipe_view.swipeGestureListener = object : SwipeGestureListener {
            override fun onSwipedLeft(@NotNull swipeActionView: SwipeActionView): Boolean {
                navigator.goToNextEntry()
                return true
            }

            override fun onSwipedRight(@NotNull swipeActionView: SwipeActionView): Boolean {
                navigator.goToPreviousEntry()
                return true
            }
        }

        updateUI()
    }

    private fun updateUI() {
        entry?.let { entry ->
            doAsync {
                entry.read = true
                App.db.entryDao().insertAll(entry)
            }
        }

        entry_view.setEntry(entry)
    }

    private fun setupToolbar() {
        toolbar.inflateMenu(R.menu.person_details)

        if (!activity.containers_layout.hasTwoColumns()) {
            toolbar.setNavigationIcon(R.drawable.ic_back_24dp)
            toolbar.setNavigationOnClickListener { activity.onBackPressed() }
        }
    }

    fun setEntry(entry: EntryWithFeed) {
        this.entry = entry
        arguments.putParcelable(ARG_ENTRY, entry)

        updateUI()
    }

    companion object {

        private val ARG_ENTRY = "ARG_ENTRY"

        fun newInstance(entry: EntryWithFeed): EntryDetailsFragment {
            val fragment = EntryDetailsFragment()
            fragment.arguments = bundleOf(ARG_ENTRY to entry)
            return fragment
        }
    }
}
