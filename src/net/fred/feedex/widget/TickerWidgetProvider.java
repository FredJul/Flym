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

package net.fred.feedex.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;

import net.fred.feedex.R;
import net.fred.feedex.activity.HomeActivity;
import net.fred.feedex.provider.FeedData;

public class TickerWidgetProvider extends AppWidgetProvider {

    private static final String ALL_UNREAD_NUMBER = new StringBuilder("(SELECT COUNT(*) FROM ")
            .append(FeedData.EntryColumns.TABLE_NAME).append(" WHERE ")
            .append(FeedData.EntryColumns.IS_READ).append(" IS NULL)").toString();


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        RemoteViews widget = createWidgetView(context);
        for (int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, widget);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    public static void updateWidget(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.updateAppWidget(new ComponentName(context.getPackageName(), TickerWidgetProvider.class.getName()), createWidgetView(context));
    }

    static RemoteViews createWidgetView(Context context) {
        RemoteViews widget = new RemoteViews(context.getPackageName(), R.layout.ticker_widget);
        widget.setOnClickPendingIntent(R.id.feed_ticker_tap_area, PendingIntent.getActivity(context, 0, new Intent(context, HomeActivity.class), 0));

        Cursor unread = context.getContentResolver().query(FeedData.EntryColumns.CONTENT_URI, new String[]{ALL_UNREAD_NUMBER}, null, null, null);
        if (unread != null) {
            if (unread.moveToFirst()) {
                widget.setTextViewText(R.id.feed_ticker, String.valueOf(unread.getInt(0)));
            }
            unread.close();
        }

        return widget;
    }
}
