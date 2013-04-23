/**
 * FeedEx
 * 
 * Copyright (c) 2012-2013 Frederic Julian
 * Copyright (c) 2010-2012 Stefan Handschuh
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

package net.fred.feedex.widget;

import net.fred.feedex.Constants;
import net.fred.feedex.PrefsManager;
import net.fred.feedex.R;
import net.fred.feedex.ThrottledContentObserver;
import net.fred.feedex.provider.FeedData.EntryColumns;
import net.fred.feedex.provider.FeedData.FeedColumns;
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

public class WidgetFeedsFactory implements RemoteViewsService.RemoteViewsFactory {
	private Context context = null;
	private final int appWidgetId;
	private Cursor cursor;
	private ThrottledContentObserver mContentObserver;

	public WidgetFeedsFactory(Context ctxt, Intent intent) {
		this.context = ctxt;
		appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
	}

	@Override
	public void onCreate() {
		computeCursor();

		mContentObserver = new ThrottledContentObserver(new Handler(), 3000) {
			@Override
			public void onChangeThrottled(boolean selfChange) {
				AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(appWidgetId, R.id.feedsListView);
			}
		};
		ContentResolver cr = context.getContentResolver();
		cr.registerContentObserver(EntryColumns.CONTENT_URI, true, mContentObserver);
	}

	@Override
	public void onDestroy() {
		cursor.close();
		ContentResolver cr = context.getContentResolver();
		cr.unregisterContentObserver(mContentObserver);
	}

	@Override
	public int getCount() {
		return cursor.getCount();
	}

	@Override
	public RemoteViews getViewAt(int position) {
		RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_item);

		if (cursor.moveToPosition(position)) {
			row.setTextViewText(android.R.id.text1, cursor.getString(0));
			row.setOnClickFillInIntent(android.R.id.text1, new Intent(Intent.ACTION_VIEW, EntryColumns.CONTENT_URI(cursor.getString(1))));

			if (!cursor.isNull(2)) {
				try {
					byte[] iconBytes = cursor.getBlob(2);

					if (iconBytes != null && iconBytes.length > 0) {
						Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);

						if (bitmap != null) {
							row.setImageViewBitmap(android.R.id.icon, bitmap);
						}
					}

				} catch (Throwable e) {
				}
			}
		}

		return row;
	}

	@Override
	public RemoteViews getLoadingView() {
		return (null);
	}

	@Override
	public int getViewTypeCount() {
		return (1);
	}

	@Override
	public long getItemId(int position) {
		return (position);
	}

	@Override
	public boolean hasStableIds() {
		return (true);
	}

	@Override
	public void onDataSetChanged() {
		cursor.close();
		computeCursor();
	}

	private void computeCursor() {
		StringBuilder selection = new StringBuilder();
		// selection.append(EntryColumns.WHERE_UNREAD);

		String feedIds = PrefsManager.getString(appWidgetId + ".feeds", "");
		if (feedIds.length() > 0) {
			if (selection.length() > 0) {
				selection.append(Constants.DB_AND);
			}
			selection.append(EntryColumns.FEED_ID).append(" IN (" + feedIds).append(')');
		}

		ContentResolver cr = context.getContentResolver();
		cursor = cr.query(EntryColumns.CONTENT_URI, new String[] { EntryColumns.TITLE, EntryColumns._ID, FeedColumns.ICON }, selection.toString(), null,
				new StringBuilder(EntryColumns.DATE).append(Constants.DB_DESC).toString());
	}
}