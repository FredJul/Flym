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

package net.fred.feedex.activity;

import net.fred.feedex.R;
import net.fred.feedex.fragment.EntriesListFragment;
import net.fred.feedex.provider.FeedData.FeedColumns;
import android.app.ActionBar;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;

public class EntriesListActivity extends FragmentActivity {
	private static final String[] FEED_PROJECTION = { FeedColumns.NAME, FeedColumns.URL, FeedColumns.ICON };

	private byte[] iconBytes = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ActionBar actionBar = getActionBar();
		if (actionBar != null)
			actionBar.setDisplayHomeAsUpEnabled(true);

		Intent intent = getIntent();
		long feedId = intent.getLongExtra(FeedColumns._ID, 0);

		String title = null;
		if (feedId > 0) {
			Cursor cursor = getContentResolver().query(FeedColumns.CONTENT_URI(feedId), FEED_PROJECTION, null, null, null);

			if (cursor.moveToFirst()) {
				title = cursor.isNull(0) ? cursor.getString(1) : cursor.getString(0);
				iconBytes = cursor.getBlob(2);
			}
			cursor.close();
		}

		if (title != null) {
			setTitle(title);
		}

		if (savedInstanceState == null) {
			EntriesListFragment fragment = new EntriesListFragment();
			Bundle args = new Bundle();
			args.putParcelable(EntriesListFragment.ARG_URI, intent.getData());
			fragment.setArguments(args);
			getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment, "fragment").commit();
		}

		if (iconBytes != null && iconBytes.length > 0) {
			int bitmapSizeInDip = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, getResources().getDisplayMetrics());
			Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
			if (bitmap != null) {
				if (bitmap.getHeight() != bitmapSizeInDip) {
					bitmap = Bitmap.createScaledBitmap(bitmap, bitmapSizeInDip, bitmapSizeInDip, false);
				}

				getActionBar().setIcon(new BitmapDrawable(getResources(), bitmap));
			}
		}

		if (MainActivity.mNotificationManager == null) {
			MainActivity.mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (MainActivity.mNotificationManager != null) {
			MainActivity.mNotificationManager.cancel(0);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		getSupportFragmentManager().findFragmentByTag("fragment").onPrepareOptionsMenu(menu);
		menu.removeItem(R.id.menu_refresh); // We do not want the refresh option here
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
		default:
			getSupportFragmentManager().findFragmentByTag("fragment").onOptionsItemSelected(item);
		}
		return true;
	}

}
