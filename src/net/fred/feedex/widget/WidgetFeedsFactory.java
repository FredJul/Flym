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
import android.util.TypedValue;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

public class WidgetFeedsFactory implements RemoteViewsService.RemoteViewsFactory {
	private Context context = null;
	private final int appWidgetId, fontSize;
	private Cursor cursor;
	private ThrottledContentObserver mContentObserver;

	public WidgetFeedsFactory(Context ctxt, Intent intent) {
		this.context = ctxt;
		appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		fontSize = intent.getIntExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, 0);
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
			row.setTextViewTextSize(android.R.id.text1, TypedValue.COMPLEX_UNIT_SP, 15 + (fontSize*2));
			Intent intent = new Intent(Intent.ACTION_VIEW, EntryColumns.CONTENT_URI(cursor.getString(1)));
			intent.putExtra(Constants.INTENT_FROM_WIDGET, true);
			row.setOnClickFillInIntent(android.R.id.content, intent);

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