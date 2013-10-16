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
import android.graphics.BitmapFactory;
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
import net.fred.feedex.provider.FeedDataContentProvider;
import net.fred.feedex.utils.UiUtils;

import java.util.Date;
import java.util.Vector;

public class EntriesCursorAdapter extends ResourceCursorAdapter {
    private int titleColumnPosition;

    private int dateColumn;
    private int isReadColumn;
    private int favoriteColumn;
    private int idColumn;
    private int feedIconColumn;
    private int feedNameColumn;
    private int linkColumn;

    private final Uri uri;
    private final boolean showFeedInfo;

    private final Vector<Long> markedAsRead = new Vector<Long>();
    private final Vector<Long> markedAsUnread = new Vector<Long>();
    private final Vector<Long> favorited = new Vector<Long>();
    private final Vector<Long> unfavorited = new Vector<Long>();

    public EntriesCursorAdapter(Context context, Uri uri, Cursor cursor, boolean showFeedInfo) {
        super(context, R.layout.item_entry_list, cursor, 0);
        this.uri = uri;
        this.showFeedInfo = showFeedInfo;

        reinit(cursor);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        final TextView textView = (TextView) view.findViewById(android.R.id.text1);
        textView.setText(cursor.getString(titleColumnPosition));

        final TextView dateTextView = (TextView) view.findViewById(android.R.id.text2);
        final ImageView starImgView = (ImageView) view.findViewById(android.R.id.icon);
        final long id = cursor.getLong(idColumn);
        view.setTag(cursor.getString(linkColumn));
        final boolean favorite = !unfavorited.contains(id) && (cursor.getInt(favoriteColumn) == 1 || favorited.contains(id));
        final CheckBox viewCheckBox = (CheckBox) view.findViewById(android.R.id.checkbox);

        starImgView.setImageResource(favorite ? R.drawable.dimmed_rating_important : R.drawable.dimmed_rating_not_important);
        starImgView.setTag(favorite ? Constants.TRUE : Constants.FALSE);
        starImgView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean newFavorite = !Constants.TRUE.equals(view.getTag());

                if (newFavorite) {
                    view.setTag(Constants.TRUE);
                    starImgView.setImageResource(R.drawable.dimmed_rating_important);
                    favorited.add(id);
                    unfavorited.remove(id);
                } else {
                    view.setTag(Constants.FALSE);
                    starImgView.setImageResource(R.drawable.dimmed_rating_not_important);
                    unfavorited.add(id);
                    favorited.remove(id);
                }

                ContentValues values = new ContentValues();
                values.put(EntryColumns.IS_FAVORITE, newFavorite ? 1 : 0);

                ContentResolver cr = MainApplication.getContext().getContentResolver();
                Uri entryUri = ContentUris.withAppendedId(uri, id);
                if (cr.update(entryUri, values, null, null) > 0) {
                    FeedDataContentProvider.notifyAllFromEntryUri(entryUri, false); //Receive New Favorite on MainActivity
                }
            }
        });

        Date date = new Date(cursor.getLong(dateColumn));

        if (showFeedInfo && feedIconColumn > -1) {
            byte[] iconBytes = cursor.getBlob(feedIconColumn);

            if (iconBytes != null && iconBytes.length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);

                if (bitmap != null) {
                    int bitmapSizeInDip = UiUtils.dpToPixel(18);
                    if (bitmap.getHeight() != bitmapSizeInDip) {
                        bitmap = Bitmap.createScaledBitmap(bitmap, bitmapSizeInDip, bitmapSizeInDip, false);
                    }
                }
                dateTextView.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(context.getResources(), bitmap), null, null, null);
            } else {
                dateTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            }
        }

        if (showFeedInfo && feedNameColumn > -1) {
            String feedName = cursor.getString(feedNameColumn);
            if (feedName != null) {
                dateTextView.setText(new StringBuilder(Constants.DATE_FORMAT.format(date)).append(' ').append(Constants.TIME_FORMAT.format(date)).append(Constants.COMMA_SPACE).append(feedName));
            } else {
                dateTextView.setText(new StringBuilder(Constants.DATE_FORMAT.format(date)).append(' ').append(Constants.TIME_FORMAT.format(date)));
            }
        } else {
            dateTextView.setText(new StringBuilder(Constants.DATE_FORMAT.format(date)).append(' ').append(Constants.TIME_FORMAT.format(date)));
        }

        viewCheckBox.setOnCheckedChangeListener(null);
        if (markedAsUnread.contains(id) || (cursor.isNull(isReadColumn) && !markedAsRead.contains(id))) {
            textView.setEnabled(true);
            dateTextView.setEnabled(true);
            viewCheckBox.setChecked(false);
        } else {
            textView.setEnabled(false);
            dateTextView.setEnabled(false);
            viewCheckBox.setChecked(true);
        }

        viewCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    markAsRead(id);
                    textView.setEnabled(false);
                    dateTextView.setEnabled(false);
                } else {
                    markAsUnread(id);
                    textView.setEnabled(true);
                    dateTextView.setEnabled(true);
                }
            }
        });
    }

    public void markAllAsRead() {
        markedAsRead.clear();
        markedAsUnread.clear();

        new Thread() {
            @Override
            public void run() {
                ContentResolver cr = MainApplication.getContext().getContentResolver();

                if (cr.update(uri, FeedData.getReadContentValues(), EntryColumns.WHERE_UNREAD, null) > 0) {
                    if (!uri.toString().startsWith(EntryColumns.CONTENT_URI.toString())) {
                        cr.notifyChange(EntryColumns.CONTENT_URI, null);
                    }
                    cr.notifyChange(FeedColumns.CONTENT_URI, null);
                    cr.notifyChange(FeedColumns.GROUPS_CONTENT_URI, null);
                    cr.notifyChange(FeedColumns.GROUPED_FEEDS_CONTENT_URI, null);
                    cr.notifyChange(EntryColumns.FAVORITES_CONTENT_URI, null);
                }
            }
        }.start();
    }

    private void markAsRead(final long id) {
        markedAsRead.add(id);
        markedAsUnread.remove(id);

        new Thread() {
            @Override
            public void run() {
                ContentResolver cr = MainApplication.getContext().getContentResolver();
                Uri entryUri = ContentUris.withAppendedId(uri, id);
                if (cr.update(entryUri, FeedData.getReadContentValues(), null, null) > 0) {
                    FeedDataContentProvider.notifyAllFromEntryUri(entryUri, false);
                }
            }
        }.start();
    }

    private void markAsUnread(final long id) {
        markedAsUnread.add(id);
        markedAsRead.remove(id);

        new Thread() {
            @Override
            public void run() {
                ContentResolver cr = MainApplication.getContext().getContentResolver();
                Uri entryUri = ContentUris.withAppendedId(uri, id);
                if (cr.update(entryUri, FeedData.getUnreadContentValues(), null, null) > 0) {
                    FeedDataContentProvider.notifyAllFromEntryUri(entryUri, false);
                }
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
        markedAsRead.clear();
        markedAsUnread.clear();
        favorited.clear();
        unfavorited.clear();

        if (cursor != null) {
            titleColumnPosition = cursor.getColumnIndex(EntryColumns.TITLE);
            dateColumn = cursor.getColumnIndex(EntryColumns.DATE);
            isReadColumn = cursor.getColumnIndex(EntryColumns.IS_READ);
            favoriteColumn = cursor.getColumnIndex(EntryColumns.IS_FAVORITE);
            idColumn = cursor.getColumnIndex(EntryColumns._ID);
            linkColumn = cursor.getColumnIndex(EntryColumns.LINK);
            if (showFeedInfo) {
                feedIconColumn = cursor.getColumnIndex(FeedColumns.ICON);
                feedNameColumn = cursor.getColumnIndex(FeedColumns.NAME);
            }
        }
    }
}
