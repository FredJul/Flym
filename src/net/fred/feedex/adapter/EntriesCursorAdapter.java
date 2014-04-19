/**
 * FeedEx
 *
 * Copyright (c) 2012-2013 Frederic Julian
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 *
 * Copyright (c) 2010-2012 Stefan Handschuh
 *
 *     Permission is hereby granted, free of charge, to any person obtaining a copy
 *     of this software and associated documentation files (the "Software"), to deal
 *     in the Software without restriction, including without limitation the rights
 *     to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *     copies of the Software, and to permit persons to whom the Software is
 *     furnished to do so, subject to the following conditions:
 *
 *     The above copyright notice and this permission notice shall be included in
 *     all copies or substantial portions of the Software.
 *
 *     THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *     IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *     FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *     AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *     LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *     OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *     THE SOFTWARE.
 */

package net.fred.feedex.adapter;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import net.fred.feedex.Constants;
import net.fred.feedex.MainApplication;
import net.fred.feedex.R;
import net.fred.feedex.provider.FeedData;
import net.fred.feedex.provider.FeedData.EntryColumns;
import net.fred.feedex.provider.FeedData.FeedColumns;
import net.fred.feedex.utils.StringUtils;
import net.fred.feedex.utils.UiUtils;

import java.util.Vector;

public class EntriesCursorAdapter extends ResourceCursorAdapter {

    private static class ViewHolder {
        public TextView titleTextView;
        public TextView dateTextView;
        public ImageView starImgView;
        public CheckBox isReadCb;
    }

    private int mTitlePos, mDatePos, mIsReadPos, mFavoritePos, mIdPos, mFeedIdPos, mFeedIconPos, mFeedNamePos;

    private final Uri mUri;
    private final boolean mShowFeedInfo;

    private final Vector<Long> mMarkedAsReadEntries = new Vector<Long>();
    private final Vector<Long> mMarkedAsUnreadEntries = new Vector<Long>();
    private final Vector<Long> mFavoriteEntries = new Vector<Long>();
    private final Vector<Long> mNotFavoriteEntries = new Vector<Long>();

    public EntriesCursorAdapter(Context context, Uri uri, Cursor cursor, boolean showFeedInfo) {
        super(context, R.layout.item_entry_list, cursor, 0);
        mUri = uri;
        mShowFeedInfo = showFeedInfo;

        reinit(cursor);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        if (view.getTag() == null) {
            ViewHolder holder = new ViewHolder();
            holder.titleTextView = (TextView) view.findViewById(android.R.id.text1);
            holder.dateTextView = (TextView) view.findViewById(android.R.id.text2);
            holder.starImgView = (ImageView) view.findViewById(android.R.id.icon);
            holder.isReadCb = (CheckBox) view.findViewById(android.R.id.checkbox);
            view.setTag(holder);
        }

        final ViewHolder holder = (ViewHolder) view.getTag();

        holder.titleTextView.setText(cursor.getString(mTitlePos));

        final long id = cursor.getLong(mIdPos);
        final boolean favorite = !mNotFavoriteEntries.contains(id) && (cursor.getInt(mFavoritePos) == 1 || mFavoriteEntries.contains(id));

        holder.starImgView.setImageResource(favorite ? R.drawable.dimmed_rating_important : R.drawable.dimmed_rating_not_important);
        holder.starImgView.setTag(favorite ? Constants.TRUE : Constants.FALSE);
        holder.starImgView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean newFavorite = !Constants.TRUE.equals(view.getTag());

                if (newFavorite) {
                    view.setTag(Constants.TRUE);
                    holder.starImgView.setImageResource(R.drawable.dimmed_rating_important);
                    mFavoriteEntries.add(id);
                    mNotFavoriteEntries.remove(id);
                } else {
                    view.setTag(Constants.FALSE);
                    holder.starImgView.setImageResource(R.drawable.dimmed_rating_not_important);
                    mNotFavoriteEntries.add(id);
                    mFavoriteEntries.remove(id);
                }

                ContentValues values = new ContentValues();
                values.put(EntryColumns.IS_FAVORITE, newFavorite ? 1 : 0);

                ContentResolver cr = MainApplication.getContext().getContentResolver();
                Uri entryUri = ContentUris.withAppendedId(mUri, id);
                cr.update(entryUri, values, null, null);
            }
        });

        if (mShowFeedInfo && mFeedIconPos > -1) {
            final long feedId = cursor.getLong(mFeedIdPos);
            Bitmap bitmap = UiUtils.getFaviconBitmap(feedId, cursor, mFeedIconPos);

            if (bitmap != null) {
                holder.dateTextView.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(context.getResources(), bitmap), null, null, null);
            } else {
                holder.dateTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            }
        }

        if (mShowFeedInfo && mFeedNamePos > -1) {
            String feedName = cursor.getString(mFeedNamePos);
            if (feedName != null) {
                holder.dateTextView.setText(new StringBuilder(feedName).append(Constants.COMMA_SPACE).append(StringUtils.getDateTimeString(cursor.getLong(mDatePos))));
            } else {
                holder.dateTextView.setText(StringUtils.getDateTimeString(cursor.getLong(mDatePos)));
            }
        } else {
            holder.dateTextView.setText(StringUtils.getDateTimeString(cursor.getLong(mDatePos)));
        }

        holder.isReadCb.setOnCheckedChangeListener(null);
        if (mMarkedAsUnreadEntries.contains(id) || (cursor.isNull(mIsReadPos) && !mMarkedAsReadEntries.contains(id))) {
            holder.titleTextView.setEnabled(true);
            holder.dateTextView.setEnabled(true);
            holder.isReadCb.setChecked(false);
        } else {
            holder.titleTextView.setEnabled(false);
            holder.dateTextView.setEnabled(false);
            holder.isReadCb.setChecked(true);
        }

        holder.isReadCb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    markAsRead(id);
                    holder.titleTextView.setEnabled(false);
                    holder.dateTextView.setEnabled(false);
                } else {
                    markAsUnread(id);
                    holder.titleTextView.setEnabled(true);
                    holder.dateTextView.setEnabled(true);
                }
            }
        });
    }

    public void markAllAsRead(final long untilDate) {
        mMarkedAsReadEntries.clear();
        mMarkedAsUnreadEntries.clear();

        new Thread() {
            @Override
            public void run() {
                ContentResolver cr = MainApplication.getContext().getContentResolver();
                String where = EntryColumns.WHERE_UNREAD + Constants.DB_AND + '(' + EntryColumns.FETCH_DATE + Constants.DB_IS_NULL + Constants.DB_OR + EntryColumns.FETCH_DATE + "<=" + untilDate + ')';
                cr.update(mUri, FeedData.getReadContentValues(), where, null);
            }
        }.start();
    }

    private void markAsRead(final long id) {
        mMarkedAsReadEntries.add(id);
        mMarkedAsUnreadEntries.remove(id);

        new Thread() {
            @Override
            public void run() {
                ContentResolver cr = MainApplication.getContext().getContentResolver();
                Uri entryUri = ContentUris.withAppendedId(mUri, id);
                cr.update(entryUri, FeedData.getReadContentValues(), null, null);
            }
        }.start();
    }

    private void markAsUnread(final long id) {
        mMarkedAsUnreadEntries.add(id);
        mMarkedAsReadEntries.remove(id);

        new Thread() {
            @Override
            public void run() {
                ContentResolver cr = MainApplication.getContext().getContentResolver();
                Uri entryUri = ContentUris.withAppendedId(mUri, id);
                cr.update(entryUri, FeedData.getUnreadContentValues(), null, null);
            }
        }.start();
    }

    @Override
    public void changeCursor(Cursor cursor) {
        reinit(cursor);
        super.changeCursor(cursor);
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        reinit(newCursor);
        return super.swapCursor(newCursor);
    }

    @Override
    public void notifyDataSetChanged() {
        reinit(null);
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        reinit(null);
        super.notifyDataSetInvalidated();
    }

    private void reinit(Cursor cursor) {
        mMarkedAsReadEntries.clear();
        mMarkedAsUnreadEntries.clear();
        mFavoriteEntries.clear();
        mNotFavoriteEntries.clear();

        if (cursor != null) {
            mTitlePos = cursor.getColumnIndex(EntryColumns.TITLE);
            mDatePos = cursor.getColumnIndex(EntryColumns.DATE);
            mIsReadPos = cursor.getColumnIndex(EntryColumns.IS_READ);
            mFavoritePos = cursor.getColumnIndex(EntryColumns.IS_FAVORITE);
            mIdPos = cursor.getColumnIndex(EntryColumns._ID);
            if (mShowFeedInfo) {
                mFeedIdPos = cursor.getColumnIndex(EntryColumns.FEED_ID);
                mFeedIconPos = cursor.getColumnIndex(FeedColumns.ICON);
                mFeedNamePos = cursor.getColumnIndex(FeedColumns.NAME);
            }
        }
    }
}
