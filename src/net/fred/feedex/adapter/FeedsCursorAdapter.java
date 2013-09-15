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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import net.fred.feedex.Constants;
import net.fred.feedex.MainApplication;
import net.fred.feedex.R;
import net.fred.feedex.UiUtils;
import net.fred.feedex.provider.FeedData.EntryColumns;
import net.fred.feedex.provider.FeedData.FeedColumns;
import net.fred.feedex.view.DragNDropExpandableListView;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class FeedsCursorAdapter extends CursorLoaderExpandableListAdapter {
    private static final String WHERE_UNREAD = EntryColumns.IS_READ + " IS NULL";

    private static final String COLON = MainApplication.getContext().getString(R.string.colon);

    private final FragmentActivity mActivity;
    private int isGroupPosition = -1;
    private int isGroupCollapsedPosition = -1;
    private int namePosition = -1;
    private int lastUpdateColumn = -1;
    private int idPosition = -1;
    private int linkPosition = -1;
    private int errorPosition = -1;
    private int iconPosition = -1;

    private DragNDropExpandableListView mListView;
    private final SparseBooleanArray mGroupInitDone = new SparseBooleanArray();

    private static final int CACHE_MAX_ENTRIES = 100;
    private final Map<Long, String> mFormattedDateCache = new LinkedHashMap<Long, String>(CACHE_MAX_ENTRIES + 1, .75F, true) {
        private static final long serialVersionUID = -3678524849080041298L;

        @Override
        public boolean removeEldestEntry(Map.Entry<Long, String> eldest) {
            return size() > CACHE_MAX_ENTRIES;
        }
    };

    public FeedsCursorAdapter(FragmentActivity activity, Uri groupUri) {
        super(activity, groupUri, R.layout.feed_list_item, R.layout.feed_list_item);

        mActivity = activity;
    }

    public void setExpandableListView(DragNDropExpandableListView listView) {
        mListView = listView;

        mListView.setOnGroupClickListener(new OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                Cursor cursor = getGroup(groupPosition);
                if (cursor.getInt(isGroupPosition) != 1) {
                    return false;
                }

                ContentValues values = new ContentValues();
                if (mListView.isGroupExpanded(groupPosition)) {
                    values.put(FeedColumns.IS_GROUP_COLLAPSED, true);
                } else {
                    values.put(FeedColumns.IS_GROUP_COLLAPSED, false);
                }
                ContentResolver cr = mActivity.getContentResolver();
                cr.update(FeedColumns.CONTENT_URI(id), values, null, null);
                return false;
            }
        });
    }

    @Override
    protected void onCursorLoaded(Context context, Cursor cursor) {
        getCursorPositions(cursor);
    }

    @Override
    protected void bindChildView(View view, Context context, Cursor cursor) {
        view.findViewById(R.id.indicator).setVisibility(View.INVISIBLE);

        TextView textView = ((TextView) view.findViewById(android.R.id.text1));
        TextView updateTextView = ((TextView) view.findViewById(android.R.id.text2));
        updateTextView.setVisibility(View.VISIBLE);

        if (cursor.isNull(errorPosition)) {
            long timestamp = cursor.getLong(lastUpdateColumn);

            // Date formatting is expensive, look at the cache
            String formattedDate = mFormattedDateCache.get(timestamp);
            if (formattedDate == null) {
                Date date = new Date(timestamp);

                formattedDate = context.getString(R.string.update) + COLON + (timestamp == 0 ? context.getString(R.string.never) : new StringBuilder(Constants.DATE_FORMAT.format(date))
                        .append(' ').append(Constants.TIME_FORMAT.format(date)));
                mFormattedDateCache.put(timestamp, formattedDate);
            }

            updateTextView.setText(formattedDate);
        } else {
            updateTextView.setText(new StringBuilder(context.getString(R.string.error)).append(COLON).append(cursor.getString(errorPosition)));
        }

        byte[] iconBytes = cursor.getBlob(iconPosition);

        if (iconBytes != null && iconBytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);

            if (bitmap != null && bitmap.getHeight() > 0 && bitmap.getWidth() > 0) {
                int bitmapSizeInDip = UiUtils.dpToPixel(18);

                if (bitmap.getHeight() != bitmapSizeInDip) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, bitmapSizeInDip, bitmapSizeInDip, false);
                }
                textView.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(context.getResources(), bitmap), null, null, null);
            } else {
                textView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            }
        } else {
            view.setTag(null);
            textView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        }

        textView.setText((cursor.isNull(namePosition) ? cursor.getString(linkPosition) : cursor.getString(namePosition)));
    }

    @Override
    protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
        ImageView indicatorImage = (ImageView) view.findViewById(R.id.indicator);

        if (cursor.getInt(isGroupPosition) == 1) {
            indicatorImage.setVisibility(View.VISIBLE);

            TextView textView = ((TextView) view.findViewById(android.R.id.text1));
            textView.setEnabled(true);
            textView.setText(cursor.getString(namePosition));
            textView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            textView.setText(cursor.getString(namePosition));

            view.findViewById(android.R.id.text2).setVisibility(View.GONE);

            final int groupPosition = cursor.getPosition();
            if (!mGroupInitDone.get(groupPosition)) {
                mGroupInitDone.put(groupPosition, true);

                boolean savedExpandedState = cursor.getInt(isGroupCollapsedPosition) != 1;
                if (savedExpandedState && !isExpanded) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mListView.expandGroup(groupPosition);
                        }
                    });
                }

                if (savedExpandedState)
                    indicatorImage.setImageResource(R.drawable.group_expanded);
                else
                    indicatorImage.setImageResource(R.drawable.group_collapsed);
            } else {
                if (isExpanded)
                    indicatorImage.setImageResource(R.drawable.group_expanded);
                else
                    indicatorImage.setImageResource(R.drawable.group_collapsed);
            }
        } else {
            bindChildView(view, context, cursor);
            indicatorImage.setVisibility(View.GONE);
        }
    }

    @Override
    protected Uri getChildrenUri(Cursor groupCursor) {
        return FeedColumns.FEEDS_FOR_GROUPS_CONTENT_URI(groupCursor.getLong(idPosition));
    }

    @Override
    public void notifyDataSetChanged() {
        getCursorPositions(null);
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetChanged(Cursor data) {
        getCursorPositions(data);
    }

    @Override
    public void notifyDataSetInvalidated() {
        getCursorPositions(null);
        super.notifyDataSetInvalidated();
    }

    private synchronized void getCursorPositions(Cursor cursor) {
        if (cursor != null && isGroupPosition == -1) {
            isGroupPosition = cursor.getColumnIndex(FeedColumns.IS_GROUP);
            isGroupCollapsedPosition = cursor.getColumnIndex(FeedColumns.IS_GROUP_COLLAPSED);
            namePosition = cursor.getColumnIndex(FeedColumns.NAME);
            lastUpdateColumn = cursor.getColumnIndex(FeedColumns.LAST_UPDATE);
            idPosition = cursor.getColumnIndex(FeedColumns._ID);
            linkPosition = cursor.getColumnIndex(FeedColumns.URL);
            errorPosition = cursor.getColumnIndex(FeedColumns.ERROR);
            iconPosition = cursor.getColumnIndex(FeedColumns.ICON);
        }
    }
}
