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
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.widget.RemoteViews;

import net.fred.feedex.Constants;
import net.fred.feedex.R;
import net.fred.feedex.activity.HomeActivity;
import net.fred.feedex.provider.FeedData;
import net.fred.feedex.utils.PrefUtils;

public class TickerWidgetProvider extends AppWidgetProvider {

    private static final String ALL_UNREAD_NUMBER = new StringBuilder("(SELECT COUNT(*) FROM ")
            .append(FeedData.EntryColumns.TABLE_NAME).append(" WHERE ")
            .append(FeedData.EntryColumns.IS_READ).append(" IS NULL)").toString();


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            Intent svcIntent = new Intent(context, WidgetService.class);

            svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            //svcIntent.putExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, PrefUtils.getInt(appWidgetId + ".fontsize", 0));
            svcIntent.setData(Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME)));

            RemoteViews widget = new RemoteViews(context.getPackageName(), R.layout.tickerwidget);
            widget.setOnClickPendingIntent(R.id.feed_icon, PendingIntent.getActivity(context, 0, new Intent(context, HomeActivity.class), 0));
            widget.setOnClickPendingIntent(R.id.feed_ticker, PendingIntent.getActivity(context, 0, new Intent(context, HomeActivity.class), 0));
            widget.setOnClickPendingIntent(R.id.feed_ticker_background, PendingIntent.getActivity(context, 0, new Intent(context, HomeActivity.class), 0));

            Cursor unread = context.getContentResolver().query(FeedData.EntryColumns.CONTENT_URI, new String[]{ALL_UNREAD_NUMBER}, null, null, null);
            if (unread != null) {
                if (unread.moveToFirst()) {
                    widget.setTextViewText(R.id.feed_ticker, String.valueOf(unread.getInt(0)));
                }
                unread.close();
            }

            appWidgetManager.updateAppWidget(appWidgetId, widget);
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    public static void updateWidget(Context context) {
        // Now we need to update the widget
        //System.out.println("updateWidget ticker widget");
        Intent intent = new Intent(context, TickerWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException ignored) {
        }
    }
}
