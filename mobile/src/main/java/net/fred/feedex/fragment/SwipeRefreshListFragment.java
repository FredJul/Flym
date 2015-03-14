/**
 * Flym
 * <p/>
 * Copyright (c) 2012-2015 Frederic Julian
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.fred.feedex.fragment;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import net.fred.feedex.view.SwipeRefreshLayout;

public abstract class SwipeRefreshListFragment extends ListFragment implements SwipeRefreshLayout.OnRefreshListener {

    private SwipeRefreshLayout mRefreshLayout;
    private ListView mListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRefreshLayout = new SwipeRefreshLayout(inflater.getContext()) {
            @Override
            public boolean canChildScrollUp() {
                return mListView != null && mListView.getFirstVisiblePosition() != 0;
            }
        };
        inflateView(inflater, mRefreshLayout, savedInstanceState);

        mListView = (ListView) mRefreshLayout.findViewById(android.R.id.list);
        if (mListView != null) {
            // HACK to be able to know when we are on the top of the list (for the swipe refresh)
            mListView.addHeaderView(new View(mListView.getContext()));
        }

        return mRefreshLayout;
    }

    abstract public View inflateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRefreshLayout.setColorScheme(android.R.color.holo_blue_bright,
                android.R.color.holo_blue_dark,
                android.R.color.holo_blue_bright,
                android.R.color.holo_blue_dark);
        mRefreshLayout.setOnRefreshListener(this);
    }

    /**
     * It shows the SwipeRefreshLayout progress
     */
    public void showSwipeProgress() {
        mRefreshLayout.setRefreshing(true);
    }

    /**
     * It shows the SwipeRefreshLayout progress
     */
    public void hideSwipeProgress() {
        mRefreshLayout.setRefreshing(false);
    }

    /**
     * Enables swipe gesture
     */
    public void enableSwipe() {
        mRefreshLayout.setEnabled(true);
    }

    /**
     * Disables swipe gesture. It prevents manual gestures but keeps the option tu show
     * refreshing programatically.
     */
    public void disableSwipe() {
        mRefreshLayout.setEnabled(false);
    }

    /**
     * Get the refreshing status
     */
    public boolean isRefreshing() {
        return mRefreshLayout.isRefreshing();
    }
}