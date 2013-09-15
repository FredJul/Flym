package net.fred.feedex.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.fred.feedex.Constants;
import net.fred.feedex.R;
import net.fred.feedex.provider.FeedData.EntryColumns;
import net.fred.feedex.provider.FeedData.FeedColumns;

public class DrawerAdapter extends BaseAdapter implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int POS_ID = 0;
    private static final int POS_NAME = 1;
    private static final int POS_IS_GROUP = 2;
    private static final int POS_ICON = 3;
    private static final int POS_UNREAD = 4;
    private static final int POS_ALL_UNREAD = 5;
    private static final int POS_FAVORITES_UNREAD = 6;

    private final FragmentActivity mActivity;
    private final LoaderManager mLoaderMgr;
    private Cursor mFeedsCursor;

    public DrawerAdapter(FragmentActivity activity, int loaderId) {
        mActivity = activity;
        mLoaderMgr = activity.getSupportLoaderManager();

        mLoaderMgr.restartLoader(loaderId, null, this);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View itemView = null;

        if (position == 0 || position == 1) {
            itemView = inflater.inflate(R.layout.drawer_list_item, parent, false);
            TextView txtTitle = (TextView) itemView.findViewById(R.id.title);
            txtTitle.setText(position == 0 ? R.string.all : R.string.favorites);

            ImageView imgIcon = (ImageView) itemView.findViewById(R.id.icon);
            imgIcon.setImageResource(position == 0 ? R.drawable.ic_statusbar_rss : R.drawable.dimmed_rating_important);

            if (mFeedsCursor != null && mFeedsCursor.moveToFirst()) {
                int unread = mFeedsCursor.getInt(position == 0 ? POS_ALL_UNREAD : POS_FAVORITES_UNREAD);
                if (unread != 0) {
                    TextView countFeed = (TextView) itemView.findViewById(R.id.menurow_counter);
                    countFeed.setText(String.valueOf(unread));
                }
            }
        } else if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - 2)) {
            String name = mFeedsCursor.getString(POS_NAME);
            boolean isGroup = mFeedsCursor.getInt(POS_IS_GROUP) == 1;

            if (isGroup) {
                itemView = inflater.inflate(R.layout.drawer_list_header, parent, false);
                TextView txtTitle = (TextView) itemView.findViewById(R.id.list_header_title);
                txtTitle.setText(name);
            } else {
                itemView = inflater.inflate(R.layout.drawer_list_item, parent, false);
                TextView txtTitle = (TextView) itemView.findViewById(R.id.title);
                txtTitle.setText(name);

                byte[] iconBytes = mFeedsCursor.getBlob(POS_ICON);
                if (iconBytes != null && iconBytes.length > 0) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
                    ImageView imgIcon = (ImageView) itemView.findViewById(R.id.icon);
                    imgIcon.setImageBitmap(bitmap);
                }

                int unread = mFeedsCursor.getInt(POS_UNREAD);
                if (unread != 0) {
                    TextView countFeed = (TextView) itemView.findViewById(R.id.menurow_counter);
                    countFeed.setText(String.valueOf(unread));
                }
            }
        }

        return itemView;
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
        CursorLoader cursorLoader = new CursorLoader(mActivity, FeedColumns.GROUPED_FEEDS_CONTENT_URI, new String[]{FeedColumns._ID, FeedColumns.NAME, FeedColumns.IS_GROUP, FeedColumns.ICON,
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