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

import net.fred.feedex.PrefsManager;
import net.fred.feedex.R;
import net.fred.feedex.provider.FeedData.FeedColumns;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.view.View;
import android.view.View.OnClickListener;

public class WidgetConfigActivity extends PreferenceActivity {
	private int widgetId;

	private static final String NAMECOLUMN = new StringBuilder("ifnull(").append(FeedColumns.NAME).append(',').append(FeedColumns.URL).append(") as title").toString();

	public static final String ZERO = "0";

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setResult(RESULT_CANCELED);

		Bundle extras = getIntent().getExtras();

		if (extras != null) {
			widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		}
		if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish();
		}
		addPreferencesFromResource(R.layout.widget_preferences);
		setContentView(R.layout.widget_config);

		final PreferenceCategory feedsPreferenceCategory = (PreferenceCategory) findPreference("widget.visiblefeeds");

		Cursor cursor = this.getContentResolver().query(FeedColumns.CONTENT_URI, new String[] { FeedColumns._ID, NAMECOLUMN }, null, null, null);

		if (cursor.moveToFirst()) {
			int[] ids = new int[cursor.getCount() + 1];

			CheckBoxPreference checkboxPreference = new CheckBoxPreference(this);

			checkboxPreference.setTitle(R.string.all_feeds);
			feedsPreferenceCategory.addPreference(checkboxPreference);
			checkboxPreference.setKey(ZERO);
			checkboxPreference.setDisableDependentsState(true);
			ids[0] = 0;
			for (int n = 1; !cursor.isAfterLast(); cursor.moveToNext(), n++) {
				checkboxPreference = new CheckBoxPreference(this);
				checkboxPreference.setTitle(cursor.getString(1));
				ids[n] = cursor.getInt(0);
				checkboxPreference.setKey(Integer.toString(ids[n]));
				feedsPreferenceCategory.addPreference(checkboxPreference);
				checkboxPreference.setDependency(ZERO);
			}
			cursor.close();

			findViewById(R.id.save_button).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					StringBuilder builder = new StringBuilder();

					for (int n = 0, i = feedsPreferenceCategory.getPreferenceCount(); n < i; n++) {
						CheckBoxPreference preference = (CheckBoxPreference) feedsPreferenceCategory.getPreference(n);

						if (preference.isChecked()) {
							if (n == 0) {
								break;
							} else {
								if (builder.length() > 0) {
									builder.append(',');
								}
								builder.append(preference.getKey());
							}
						}
					}

					String feedIds = builder.toString();

					PrefsManager.putString(widgetId + ".feeds", feedIds);

					int color = PrefsManager.getInteger("widget.background", WidgetProvider.STANDARD_BACKGROUND);

					PrefsManager.putInteger(widgetId + ".background", color);

					AppWidgetManager.getInstance(WidgetConfigActivity.this).notifyAppWidgetViewDataChanged(widgetId, R.id.feedsListView);
					// WidgetProvider.updateAppWidget(WidgetConfigActivity.this, widgetId, hideRead, feedIds, color);
					setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId));
					finish();
				}
			});
		} else {
			// no feeds found --> use all feeds, no dialog needed
			cursor.close();
			setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId));
		}
	}

}
