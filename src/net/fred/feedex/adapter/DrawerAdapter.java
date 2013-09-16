/**
 * FeedEx
 *
 * Copyright (c) 2012-2013 Frederic Julian
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.fred.feedex.adapter;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.fred.feedex.Constants;
import net.fred.feedex.R;
import net.fred.feedex.UiUtils;
import net.fred.feedex.provider.FeedData.EntryColumns;
import net.fred.feedex.provider.FeedData.FeedColumns;

public class DrawerAdapter extends BaseAdapter implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int POS_ID = 0;
    private static final int POS_NAME = 1;
    private static final int POS_IS_GROUP = 2;
    private static final int POS_GROUP_ID = 3;
    private static final int POS_ICON = 4;
    private static final int POS_UNREAD = 5;
    private static final int POS_ALL_UNREAD = 6;
    private static final int POS_FAVORITES_UNREAD = 7;

    private static final int ITEM_PADDING = UiUtils.dpToPixel(20);
    private static final int NORMAL_TEXT_COLOR = Color.parseColor("#EEEEEE");
    private static final int GROUP_TEXT_COLOR = Color.parseColor("#BBBBBB");

    private final Activity mActivity;
    private final LoaderManager mLoaderMgr;
    private Cursor mFeedsCursor;

    private static class ViewHolder {
        public ImageView iconView;
        public TextView titleTxt;
        public TextView unreadTxt;
        public View separator;
    }

    public DrawerAdapter(Activity activity, int loaderId) {
        mActivity = activity;
        mLoaderMgr = activity.getLoaderManager();

        mLoaderMgr.restartLoader(loaderId, null, this);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.drawer_list_item, parent, false);

            ViewHolder holder = new ViewHolder();
            holder.iconView = (ImageView) convertView.findViewById(R.id.icon);
            holder.titleTxt = (TextView) convertView.findViewById(R.id.title);
            holder.unreadTxt = (TextView) convertView.findViewById(R.id.unread_count);
            holder.separator = convertView.findViewById(R.id.separator);
            convertView.setTag(holder);
        }

        ViewHolder holder = (ViewHolder) convertView.getTag();

        // default init
        holder.iconView.setImageDrawable(null);
        holder.titleTxt.setText("");
        holder.titleTxt.setTextColor(NORMAL_TEXT_COLOR);
        holder.titleTxt.setAllCaps(false);
        holder.unreadTxt.setText("");
        convertView.setPadding(0, 0, 0, 0);
        holder.separator.setVisibility(View.GONE);

        if (position == 0 || position == 1) {
            holder.titleTxt.setText(position == 0 ? R.string.all : R.string.favorites);
            holder.iconView.setImageResource(position == 0 ? R.drawable.ic_statusbar_rss : R.drawable.dimmed_rating_important);

            if (mFeedsCursor != null && mFeedsCursor.moveToFirst()) {
                int unread = mFeedsCursor.getInt(position == 0 ? POS_ALL_UNREAD : POS_FAVORITES_UNREAD);
                if (unread != 0) {
                    holder.unreadTxt.setText(String.valueOf(unread));
                }
            }
        } else if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - 2)) {
            holder.titleTxt.setText(mFeedsCursor.getString(POS_NAME));

            if (mFeedsCursor.getInt(POS_IS_GROUP) == 1) {
                holder.titleTxt.setTextColor(GROUP_TEXT_COLOR);
                holder.titleTxt.setAllCaps(true);
                holder.separator.setVisibility(View.VISIBLE);
            } else {
                byte[] iconBytes = mFeedsCursor.getBlob(POS_ICON);
                if (iconBytes != null && iconBytes.length > 0) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
                    holder.iconView.setImageBitmap(bitmap);
                } else {
                    holder.iconView.setImageResource(R.drawable.icon);
                }

                int unread = mFeedsCursor.getInt(POS_UNREAD);
                if (unread != 0) {
                    holder.unreadTxt.setText(String.valueOf(unread));
                }
            }

            if (!mFeedsCursor.isNull(POS_GROUP_ID)) { // First level
                convertView.setPadding(ITEM_PADDING, 0, 0, 0);
            }
        }

        return convertView;
    }

    @Override
    public int getCount() {
        if (mFeedsCursor != null) {
            return mFeedsCursor.getCount() + 2;
        }
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - 2)) {
            return mFeedsCursor.getLong(POS_ID);
        }

        return -1;
    }

    public String getItemName(int position) {
        if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - 2)) {
            return mFeedsCursor.getString(POS_NAME);
        }

        return null;
    }

    public boolean isItemAGroup(int position) {
        if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - 2)) {
            return mFeedsCursor.getInt(POS_IS_GROUP) == 1;
        }

        return false;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        CursorLoader cursorLoader = new CursorLoader(mActivity, FeedColumns.GROUPED_FEEDS_CONTENT_URI, new String[]{FeedColumns._ID, FeedColumns.NAME, FeedColumns.IS_GROUP, FeedColumns.GROUP_ID, FeedColumns.ICON,
                "(SELECT COUNT(*) FROM " + EntryColumns.TABLE_NAME + " WHERE " + EntryColumns.IS_READ + " IS NULL AND " + EntryColumns.FEED_ID + "=" + FeedColumns.TABLE_NAME + "." + FeedColumns._ID + ")",
                "(SELECT COUNT(*) FROM " + EntryColumns.TABLE_NAME + " WHERE " + EntryColumns.IS_READ + " IS NULL)",
                "(SELECT COUNT(*) FROM " + EntryColumns.TABLE_NAME + " WHERE " + EntryColumns.IS_READ + " IS NULL AND " + EntryColumns.IS_FAVORITE + Constants.DB_IS_TRUE + ")"}, null, null, null);
        cursorLoader.setUpdateThrottle(Constants.UPDATE_THROTTLE_DELAY);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mFeedsCursor = cursor;
        notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }
}