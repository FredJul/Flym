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

package net.fred.feedex.widget;

import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import net.fred.feedex.Constants;
import net.fred.feedex.R;
import net.fred.feedex.provider.FeedData;
import net.fred.feedex.provider.FeedData.EntryColumns;
import net.fred.feedex.utils.PrefUtils;
import net.fred.feedex.utils.ThrottledContentObserver;

public class WidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return (new WidgetFeedsFactory(this.getApplicationContext(), intent));
    }
}

class WidgetFeedsFactory implements RemoteViewsService.RemoteViewsFactory {
    private final int mAppWidgetId, mFontSize;
    private Context mContext = null;
    private Cursor mCursor;
    private ThrottledContentObserver mContentObserver;

    public WidgetFeedsFactory(Context ctxt, Intent intent) {
        this.mContext = ctxt;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        mFontSize = intent.getIntExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, 0);
    }

    @Override
    public void onCreate() {
        computeCursor();

        mContentObserver = new ThrottledContentObserver(new Handler(), 3000) {
            @Override
            public void onChangeThrottled() {
                AppWidgetManager.getInstance(mContext).notifyAppWidgetViewDataChanged(mAppWidgetId, R.id.feedsListView);
            }
        };
        ContentResolver cr = mContext.getContentResolver();
        cr.registerContentObserver(EntryColumns.ALL_ENTRIES_CONTENT_URI, true, mContentObserver);
    }

    @Override
    public void onDestroy() {
        mCursor.close();
        ContentResolver cr = mContext.getContentResolver();
        cr.unregisterContentObserver(mContentObserver);
    }

    @Override
    public int getCount() {
        return mCursor.getCount();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews row = new RemoteViews(mContext.getPackageName(), R.layout.widget_item);

        if (mCursor.moveToPosition(position)) {
            row.setTextViewText(android.R.id.text1, mCursor.getString(0));
            row.setFloat(android.R.id.text1, "setTextSize", 15 + (mFontSize * 3));
            Intent intent = new Intent(Intent.ACTION_VIEW, EntryColumns.ALL_ENTRIES_CONTENT_URI(mCursor.getString(1)));
            intent.putExtra(Constants.INTENT_FROM_WIDGET, true);
            row.setOnClickFillInIntent(android.R.id.content, intent);

            row.setImageViewResource(android.R.id.icon, R.mipmap.ic_launcher);
            if (!mCursor.isNull(2)) {
                try {
                    byte[] iconBytes = mCursor.getBlob(2);

                    if (iconBytes != null && iconBytes.length > 0) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);

                        if (bitmap != null) {
                            row.setImageViewBitmap(android.R.id.icon, bitmap);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        return row;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onDataSetChanged() {
        mCursor.close();
        computeCursor();
    }

    private void computeCursor() {
        StringBuilder selection = new StringBuilder();
        // selection.append(EntryColumns.WHERE_UNREAD);

        String feedIds = PrefUtils.getString(mAppWidgetId + ".feeds", "");
        if (feedIds.length() > 0) {
            if (selection.length() > 0) {
                selection.append(Constants.DB_AND);
            }
            selection.append(EntryColumns.FEED_ID).append(" IN (").append(feedIds).append(')');
        }

        ContentResolver cr = mContext.getContentResolver();
        mCursor = cr.query(EntryColumns.ALL_ENTRIES_CONTENT_URI, new String[]{EntryColumns.TITLE, EntryColumns._ID, FeedData.FeedColumns.ICON}, selection.toString(), null,
                EntryColumns.DATE + Constants.DB_DESC);
    }
}