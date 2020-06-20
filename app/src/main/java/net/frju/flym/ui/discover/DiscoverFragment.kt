package net.frju.flym.ui.discover

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.fred.feedex.R


class DiscoverFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_discover, container, false)
        return view
    }

    companion object {

        const val TAG = "DiscoverFragment"

        @JvmStatic
        fun newInstance() = DiscoverFragment()
    }
}