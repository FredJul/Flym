/**
 * Flym
 *
 *
 * Copyright (c) 2012-2015 Frederic Julian
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package net.frju.flym.ui.items

import android.arch.lifecycle.LifecycleFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.fred.feedex.R
import net.frju.flym.ui.views.SwipeRefreshLayout

abstract class SwipeRefreshFragment : LifecycleFragment(), SwipeRefreshLayout.OnRefreshListener {

    private var refreshLayout: SwipeRefreshLayout? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        refreshLayout = SwipeRefreshLayout(inflater!!.context)
        inflateView(inflater, refreshLayout!!, savedInstanceState)

        return refreshLayout
    }

    abstract fun inflateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        refreshLayout?.setColorScheme(R.color.colorAccent,
                R.color.colorPrimaryDark,
                R.color.colorAccent,
                R.color.colorPrimaryDark)
        refreshLayout?.setOnRefreshListener(this)
    }

    /**
     * It shows the SwipeRefreshLayout progress
     */
    fun showSwipeProgress() {
        refreshLayout?.isRefreshing = true
    }

    /**
     * It shows the SwipeRefreshLayout progress
     */
    fun hideSwipeProgress() {
        refreshLayout?.isRefreshing = false
    }

    /**
     * Enables swipe gesture
     */
    fun enableSwipe() {
        refreshLayout?.isEnabled = true
    }

    /**
     * Disables swipe gesture. It prevents manual gestures but keeps the option tu show
     * refreshing programatically.
     */
    fun disableSwipe() {
        refreshLayout?.isEnabled = false
    }

    /**
     * Get the refreshing status
     */
    val isRefreshing: Boolean
        get() = refreshLayout?.isRefreshing ?: false
}