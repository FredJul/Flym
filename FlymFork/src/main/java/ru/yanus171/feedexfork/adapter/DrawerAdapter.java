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

package ru.yanus171.feedexfork.adapter;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.LinkedHashMap;
import java.util.Map;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.StringUtils;
import ru.yanus171.feedexfork.utils.Timer;
import ru.yanus171.feedexfork.utils.UiUtils;

public class DrawerAdapter extends BaseAdapter {

    private static final int POS_ID = 0;
    private static final int POS_URL = 1;
    private static final int POS_NAME = 2;
    private static final int POS_IS_GROUP = 3;
    private static final int POS_ICON = 4;
    private static final int POS_LAST_UPDATE = 5;
    private static final int POS_ERROR = 6;
    private static final int POS_UNREAD = 7;
    private static final int POS_ALL = 8;
    private static final int POS_IS_SHOW_TEXT_IN_ENTRY_LIST = 9;
    private static final int POS_IS_GROUP_EXPANDED = 10;
    private static final int POS_IS_AUTO_RESRESH = 11;

    public static final int FIRST_ENTRY_POS = 4;

    private static final int NORMAL_TEXT_COLOR = Color.parseColor("#EEEEEE");
    private static final int GROUP_TEXT_COLOR = Color.parseColor("#BBBBBB");

    private static final String COLON = MainApplication.getContext().getString(R.string.colon);

    private static final int CACHE_MAX_ENTRIES = 100;
    private final Map<Long, String> mFormattedDateCache = new LinkedHashMap<Long, String>(CACHE_MAX_ENTRIES + 1, .75F, true) {
        @Override
        public boolean removeEldestEntry(Map.Entry<Long, String> eldest) {
            return size() > CACHE_MAX_ENTRIES;
        }
    };

    private final Context mContext;
    private Cursor mFeedsCursor;
    private int mAllUnreadNumber, mFavoritesNumber, mAllNumber;

    public DrawerAdapter(Context context, Cursor feedCursor) {
        mContext = context;
        mFeedsCursor = feedCursor;

        updateNumbers();
    }

    public void setCursor(Cursor feedCursor) {
        mFeedsCursor = feedCursor;

        updateNumbers();
        notifyDataSetChanged();
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_drawer_list, parent, false);

            ViewHolder holder = new ViewHolder();
            holder.iconView = (ImageView) convertView.findViewById(android.R.id.icon);
            holder.titleTxt = (TextView) convertView.findViewById(android.R.id.text1);
            holder.stateTxt = (TextView) convertView.findViewById(android.R.id.text2);
            holder.unreadTxt = (TextView) convertView.findViewById(R.id.unread_count);
            holder.allTxt = (TextView) convertView.findViewById(R.id.all_count);
            holder.autoRefreshIcon = (ImageView) convertView.findViewById(R.id.auto_refresh_icon);
            holder.separator = convertView.findViewById(R.id.separator);
            convertView.setTag(R.id.holder, holder);
        }

        ViewHolder holder = (ViewHolder) convertView.getTag(R.id.holder);

        // default init
        holder.iconView.setImageDrawable(null);
        holder.titleTxt.setText("");
        holder.titleTxt.setTextColor(NORMAL_TEXT_COLOR);
        holder.titleTxt.setAllCaps(false);
        holder.titleTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18 + PrefUtils.getFontSizeEntryList() );
        holder.stateTxt.setVisibility(View.GONE);
        holder.unreadTxt.setText("");
        holder.allTxt.setText("");
        convertView.setPadding(0, 0, 0, 0);
        holder.separator.setVisibility(View.GONE);
        holder.autoRefreshIcon.setVisibility( View.GONE );

        if (position == 0 || position == 1 || position == 2 || position == 3 ) {
            switch (position) {
                case 0:
                    holder.titleTxt.setText(R.string.unread_entries);
                    holder.iconView.setImageResource(R.drawable.ic_statusbar_rss);
                    if (mAllUnreadNumber != 0)
                        holder.unreadTxt.setText(String.valueOf(mAllUnreadNumber));
                    break;
                case 1:
                    holder.titleTxt.setText(R.string.all_entries);
                    holder.iconView.setImageResource(R.drawable.ic_statusbar_rss);
                    if (mAllNumber != 0)
                        holder.unreadTxt.setText(String.valueOf(mAllNumber));
                    break;
                case 2:
                    holder.titleTxt.setText(R.string.favorites);
                    holder.iconView.setImageResource(R.drawable.rating_important);
                    if (mFavoritesNumber != 0)
                        holder.unreadTxt.setText(String.valueOf(mFavoritesNumber));
                    break;
                case 3:
                    holder.titleTxt.setText(R.string.externalLinks);
                    holder.iconView.setImageResource(R.drawable.ic_statusbar_rss);
                    break;
            }
        }
        if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - FIRST_ENTRY_POS)) {
            holder.titleTxt.setText((mFeedsCursor.isNull(POS_NAME) ? mFeedsCursor.getString(POS_URL) : mFeedsCursor.getString(POS_NAME)));

            if (mFeedsCursor.getInt(POS_IS_GROUP) == 1) {
                holder.iconView.setImageResource(isGroupExpanded(position) ? R.drawable.group_expanded : R.drawable.group_collapsed);
                holder.iconView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ContentResolver cr = MainApplication.getContext().getContentResolver();
                        ContentValues values = new ContentValues();
                        values.put(FeedData.FeedColumns.IS_GROUP_EXPANDED, isGroupExpanded(position) ? null : 1 );
                        cr.update(FeedData.FeedColumns.CONTENT_URI(getItemId(position)), values, null, null);
                    }
                });
                holder.titleTxt.setTextColor(GROUP_TEXT_COLOR);
                holder.titleTxt.setAllCaps(true);
                holder.separator.setVisibility(View.VISIBLE);
            } else {
                holder.stateTxt.setVisibility(View.VISIBLE);

                if (mFeedsCursor.isNull(POS_ERROR)) {
                    long timestamp = mFeedsCursor.getLong(POS_LAST_UPDATE);

                    // Date formatting is expensive, look at the cache
                    String formattedDate = mFormattedDateCache.get(timestamp);
                    if (formattedDate == null) {

                        formattedDate = mContext.getString(R.string.update) + COLON;

                        if (timestamp == 0) {
                            formattedDate += mContext.getString(R.string.never);
                        } else {
                            formattedDate += StringUtils.getDateTimeString(timestamp);
                        }

                        mFormattedDateCache.put(timestamp, formattedDate);
                    }

                    holder.stateTxt.setText(formattedDate);
                } else {
                    holder.stateTxt.setText(new StringBuilder(mContext.getString(R.string.error)).append(COLON).append(mFeedsCursor.getString(POS_ERROR)));
                }

                final long feedId = mFeedsCursor.getLong(POS_ID);
                Bitmap bitmap = UiUtils.getFaviconBitmap(feedId, mFeedsCursor, POS_ICON);

                if (bitmap != null) {
                    holder.iconView.setImageBitmap(bitmap);
                } else {
                    holder.iconView.setImageResource(R.mipmap.ic_launcher);
                }

                int unread = mFeedsCursor.getInt(POS_UNREAD);
                if (unread != 0) {
                    holder.unreadTxt.setText(String.valueOf(unread));
                }

                int read = mFeedsCursor.getInt(POS_ALL) - mFeedsCursor.getInt(POS_UNREAD);
                holder.allTxt.setText(read > 0 && PrefUtils.getBoolean( "show_read_article_count", false ) ? String.valueOf(read) : "");

                holder.autoRefreshIcon.setVisibility( isAutoRefresh( position )  ? View.VISIBLE : View.GONE );
            }
        }

        return convertView;
    }

    @Override
    public int getCount() {
        if (mFeedsCursor != null) {
            return mFeedsCursor.getCount() + FIRST_ENTRY_POS;
        }
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - FIRST_ENTRY_POS)) {
            return mFeedsCursor.getLong(POS_ID);
        }

        return -1;
    }

    public int getItemPosition( long feedID ) {
        for( int i = 0; i < getCount(); i++ )
            if ( getItemId( i ) == feedID )
                return i;
        return -1;
    }

    public byte[] getItemIcon(int position) {
        if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - FIRST_ENTRY_POS)) {
            return mFeedsCursor.getBlob(POS_ICON);
        }

        return null;
    }

    public String getItemName(int position) {
        if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - FIRST_ENTRY_POS)) {
            return mFeedsCursor.isNull(POS_NAME) ? mFeedsCursor.getString(POS_URL) : mFeedsCursor.getString(POS_NAME);
        }

        return null;
    }

    public boolean isItemAGroup(int position) {
        return mFeedsCursor != null && mFeedsCursor.moveToPosition(position - FIRST_ENTRY_POS) && mFeedsCursor.getInt(POS_IS_GROUP) == 1;

    }

    public boolean isShowTextInEntryList(int position) {
        return mFeedsCursor != null && mFeedsCursor.moveToPosition(position - FIRST_ENTRY_POS) && mFeedsCursor.getInt(POS_IS_SHOW_TEXT_IN_ENTRY_LIST) == 1;

    }

    private boolean isGroupExpanded(int position) {
        return mFeedsCursor != null && mFeedsCursor.moveToPosition(position - FIRST_ENTRY_POS) && mFeedsCursor.getInt(POS_IS_GROUP_EXPANDED) == 1;

    }

    private boolean isAutoRefresh(int position) {
        return mFeedsCursor != null && mFeedsCursor.moveToPosition(position - FIRST_ENTRY_POS) && mFeedsCursor.getInt(POS_IS_AUTO_RESRESH) == 1;

    }

    private void updateNumbers() {
        mAllUnreadNumber = mFavoritesNumber = mAllNumber = 0;
        Timer timer = new Timer( "updateNumbers()" );
        // Gets the numbers of entries (should be in a thread, but it's way easier like this and it shouldn't be so slow)
        Cursor numbers = mContext.getContentResolver().query(EntryColumns.CONTENT_URI, new String[]{FeedData.ALL_UNREAD_NUMBER, FeedData.FAVORITES_NUMBER, FeedData.ALL_NUMBER, }, null, null, null);
        if (numbers != null) {
            if (numbers.moveToFirst()) {
                mAllUnreadNumber = numbers.getInt(0);
                mFavoritesNumber = numbers.getInt(1);
                mAllNumber = numbers.getInt( 2 );
            }
            numbers.close();
        }
        timer.End();
    }

    private static class ViewHolder {
        public ImageView iconView;
        public TextView titleTxt;
        public TextView stateTxt;
        public TextView unreadTxt;
        public TextView allTxt;
        public ImageView autoRefreshIcon;

        public View separator;
    }
}
