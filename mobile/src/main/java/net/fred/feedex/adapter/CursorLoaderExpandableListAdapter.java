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

package net.fred.feedex.adapter;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import net.fred.feedex.Constants;

/**
 * A fairly simple ExpandableListAdapter that creates views defined in an XML file. You can specify the XML file that defines the appearance of the views.
 */
public abstract class CursorLoaderExpandableListAdapter extends BaseExpandableListAdapter {
    private static final String URI_ARG = "uri";
    private final Activity mActivity;
    private final LoaderManager.LoaderCallbacks<Cursor> mChildrenLoaderCallback = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            CursorLoader cursorLoader = new CursorLoader(mActivity, (Uri) args.getParcelable(URI_ARG), null, null, null, null) {

                @Override
                public Cursor loadInBackground() {
                    Cursor c = super.loadInBackground();
                    onCursorLoaded(mActivity, c);
                    return c;
                }

            };
            cursorLoader.setUpdateThrottle(Constants.UPDATE_THROTTLE_DELAY);
            return cursorLoader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mChildrenCursors.put(loader.getId() - 1, new Pair<>(data, false));
            notifyDataSetChanged();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mChildrenCursors.delete(loader.getId() - 1);
            notifyDataSetInvalidated();
        }
    };
    private final LoaderManager mLoaderMgr;
    private final Uri mGroupUri;
    private final int mCollapsedGroupLayout;
    private final int mExpandedGroupLayout;
    private final int mChildLayout;
    private final LayoutInflater mInflater;
    /**
     * The map of a group position to the group's children cursor
     */
    private final SparseArray<Pair<Cursor, Boolean>> mChildrenCursors = new SparseArray<>();
    private Cursor mGroupCursor;
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupLoaderCallback = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            CursorLoader cursorLoader = new CursorLoader(mActivity, mGroupUri, null, null, null, null) {

                @Override
                public Cursor loadInBackground() {
                    Cursor c = super.loadInBackground();
                    onCursorLoaded(mActivity, c);
                    return c;
                }

            };
            cursorLoader.setUpdateThrottle(Constants.UPDATE_THROTTLE_DELAY);
            return cursorLoader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mGroupCursor = data;
            setAllChildrenCursorsAsObsolete();
            notifyDataSetChanged();
            notifyDataSetChanged(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mGroupCursor = null;
            setAllChildrenCursorsAsObsolete();
            notifyDataSetInvalidated();
        }
    };

    /**
     * Constructor.
     *
     * @param collapsedGroupLayout resource identifier of a layout file that defines the views for collapsed groups.
     * @param expandedGroupLayout  resource identifier of a layout file that defines the views for expanded groups.
     * @param childLayout          resource identifier of a layout file that defines the views for all children but the last..
     */
    public CursorLoaderExpandableListAdapter(Activity activity, Uri groupUri, int collapsedGroupLayout, int expandedGroupLayout, int childLayout) {
        mActivity = activity;
        mLoaderMgr = activity.getLoaderManager();
        mGroupUri = groupUri;

        mCollapsedGroupLayout = collapsedGroupLayout;
        mExpandedGroupLayout = expandedGroupLayout;
        mChildLayout = childLayout;

        mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mLoaderMgr.restartLoader(0, null, mGroupLoaderCallback);
    }

    /**
     * Constructor.
     *
     * @param groupLayout resource identifier of a layout file that defines the views for all groups.
     * @param childLayout resource identifier of a layout file that defines the views for all children.
     */
    public CursorLoaderExpandableListAdapter(Activity activity, Uri groupUri, int groupLayout, int childLayout) {
        this(activity, groupUri, groupLayout, groupLayout, childLayout);
    }

    private void setAllChildrenCursorsAsObsolete() {
        int key;
        for (int i = 0; i < mChildrenCursors.size(); i++) {
            key = mChildrenCursors.keyAt(i);
            mChildrenCursors.put(key, new Pair<>(mChildrenCursors.get(key).first, true));
        }
    }

    /**
     * Makes a new child view to hold the data pointed to by cursor.
     *
     * @param parent The parent to which the new view is attached to
     * @return the newly created view.
     */
    public View newChildView(ViewGroup parent) {
        return mInflater.inflate(mChildLayout, parent, false);
    }

    /**
     * Makes a new group view to hold the group data pointed to by cursor.
     *
     * @param isExpanded Whether the group is expanded.
     * @param parent     The parent to which the new view is attached to
     * @return The newly created view.
     */
    public View newGroupView(boolean isExpanded, ViewGroup parent) {
        return mInflater.inflate((isExpanded) ? mExpandedGroupLayout : mCollapsedGroupLayout, parent, false);
    }

    /**
     * Gets the Cursor for the children at the given group. Subclasses must implement this method to return the children data for a particular group.
     * <p/>
     * If you want to asynchronously query a provider to prevent blocking the UI, it is possible to return null and at a later time call setChildrenCursor(int, Cursor).
     * <p/>
     * It is your responsibility to manage this Cursor through the Activity lifecycle. It is a good idea to use {@link Activity#managedQuery} which will handle this for you. In some situations, the
     * adapter will deactivate the Cursor on its own, but this will not always be the case, so please ensure the Cursor is properly managed.
     *
     * @param groupCursor The cursor pointing to the group whose children cursor should be returned
     * @return The cursor for the children of a particular group, or null.
     */
    abstract protected Uri getChildrenUri(Cursor groupCursor);

    @Override
    public Cursor getChild(int groupPosition, int childPosition) {
        // Return this group's children Cursor pointing to the particular child
        Pair<Cursor, Boolean> childCursor = mChildrenCursors.get(groupPosition);
        if (childCursor != null && !childCursor.first.isClosed()) {
            childCursor.first.moveToPosition(childPosition);
            return childCursor.first;
        }

        return null;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        Pair<Cursor, Boolean> childrenCursor = mChildrenCursors.get(groupPosition);
        if (childrenCursor != null && !childrenCursor.first.isClosed() && childrenCursor.first.moveToPosition(childPosition)) {
            return childrenCursor.first.getLong(childrenCursor.first.getColumnIndex("_id"));
        }

        return 0;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        Pair<Cursor, Boolean> cursor = mChildrenCursors.get(groupPosition);
        if (cursor == null || cursor.first.isClosed() || !cursor.first.moveToPosition(childPosition)) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }

        View v;
        if (convertView == null) {
            v = newChildView(parent);
        } else {
            v = convertView;
        }
        bindChildView(v, mActivity, cursor.first);
        return v;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        Pair<Cursor, Boolean> childCursor = mChildrenCursors.get(groupPosition);

        // We need to restart the loader
        if ((childCursor == null || childCursor.second) && mGroupCursor != null && !mGroupCursor.isClosed() && mGroupCursor.moveToPosition(groupPosition)) {
            Bundle args = new Bundle();
            args.putParcelable(URI_ARG, getChildrenUri(mGroupCursor));
            mLoaderMgr.restartLoader(groupPosition + 1, args, mChildrenLoaderCallback);
        }

        if (childCursor != null && !childCursor.first.isClosed()) {
            return childCursor.first.getCount();
        }

        return 0;
    }

    @Override
    public Cursor getGroup(int groupPosition) {
        // Return the group Cursor pointing to the given group
        if (mGroupCursor != null && !mGroupCursor.isClosed()) {
            mGroupCursor.moveToPosition(groupPosition);
        }
        return mGroupCursor;
    }

    @Override
    public int getGroupCount() {
        if (mGroupCursor != null && !mGroupCursor.isClosed()) {
            return mGroupCursor.getCount();
        }

        return 0;
    }

    @Override
    public long getGroupId(int groupPosition) {
        if (mGroupCursor != null && !mGroupCursor.isClosed() && mGroupCursor.moveToPosition(groupPosition)) {
            return mGroupCursor.getLong(mGroupCursor.getColumnIndex("_id"));
        }

        return 0;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (mGroupCursor == null || mGroupCursor.isClosed() || !mGroupCursor.moveToPosition(groupPosition)) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }

        View v;
        if (convertView == null) {
            v = newGroupView(isExpanded, parent);
        } else {
            v = convertView;
        }
        bindGroupView(v, mActivity, mGroupCursor, isExpanded);
        return v;
    }

    /**
     * Bind an existing view to the group data pointed to by cursor.
     *
     * @param view       Existing view, returned earlier by newGroupView.
     * @param context    Interface to application's global information
     * @param cursor     The cursor from which to get the data. The cursor is already moved to the correct position.
     * @param isExpanded Whether the group is expanded.
     */
    protected abstract void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded);

    /**
     * Bind an existing view to the child data pointed to by cursor
     *
     * @param view    Existing view, returned earlier by newChildView
     * @param context Interface to application's global information
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the correct position.
     */
    protected abstract void bindChildView(View view, Context context, Cursor cursor);

    /**
     * Called on the background thread just after loaded the cursor
     *
     * @param context Interface to application's global information
     * @param cursor  The cursor from which to get the data.
     */
    protected abstract void onCursorLoaded(Context context, Cursor cursor);

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onGroupCollapsed(int groupPosition) {
        mLoaderMgr.destroyLoader(groupPosition + 1);
        mChildrenCursors.delete(groupPosition);
    }

    /**
     * Notifies a data set change.
     *
     * @param data the new cursor
     */
    public void notifyDataSetChanged(Cursor data) {
    }
}
