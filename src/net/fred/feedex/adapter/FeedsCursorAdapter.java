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

package net.fred.feedex.adapter;

import java.util.Date;
import java.util.Vector;

import net.fred.feedex.Constants;
import net.fred.feedex.MainApplication;
import net.fred.feedex.R;
import net.fred.feedex.provider.FeedData.EntryColumns;
import net.fred.feedex.provider.FeedData.FeedColumns;
import net.fred.feedex.view.DragNDropExpandableListView;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class FeedsCursorAdapter extends CursorLoaderExpandableListAdapter {
	private static final String COUNT_UNREAD = "COUNT(*) - COUNT(" + EntryColumns.IS_READ + ")";
	private static final String COLON = MainApplication.getAppContext().getString(R.string.colon);

	private final FragmentActivity mActivity;
	private int isGroupPosition;
	private int isGroupCollapsedPosition;
	private int namePosition;
	private int lastUpdateColumn;
	private int idPosition;
	private int linkPosition;
	private int errorPosition;
	private int iconPosition;

	private boolean feedSort;

	private DragNDropExpandableListView mListView;
	private final SparseBooleanArray mGroupInitDone = new SparseBooleanArray();

	private final Vector<View> sortViews = new Vector<View>();
	
    private long mSelectedFeedId = -1; 

	public FeedsCursorAdapter(FragmentActivity activity, Uri groupUri) {
		super(activity, groupUri, R.layout.feed_list_item, R.layout.feed_list_item);

		mActivity = activity;
	}

	public void setExpandableListView(DragNDropExpandableListView listView) {
		mListView = listView;
		mListView.setDragNDropEnabled(feedSort);

		mListView.setOnGroupClickListener(new OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				Cursor cursor = getGroup(groupPosition);
				if (cursor.getInt(isGroupPosition) != 1) {
					startFeedActivity(id);
					return false;
				}

				ContentValues values = new ContentValues();
				if (mListView.isGroupExpanded(groupPosition)) {
					values.put(FeedColumns.IS_GROUP_COLLAPSED, true);
				} else {
					values.put(FeedColumns.IS_GROUP_COLLAPSED, false);
				}
				ContentResolver cr = mActivity.getContentResolver();
				cr.update(FeedColumns.CONTENT_URI(id), values, null, null);
				return false;
			}
		});
	}

	@Override
	protected void bindChildView(View view, Context context, Cursor cursor) {
		view.findViewById(R.id.indicator).setVisibility(View.INVISIBLE);

		TextView textView = ((TextView) view.findViewById(android.R.id.text1));
		long feedId = cursor.getLong(idPosition);
		if (feedId == mSelectedFeedId) {
			view.setBackgroundResource(android.R.color.holo_blue_dark);
		}
		else {
			view.setBackgroundResource(android.R.color.transparent);
		}

		Cursor countCursor = context.getContentResolver().query(EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(feedId),
				new String[] { COUNT_UNREAD }, null, null, null);
		countCursor.moveToFirst();
		int unreadCount = countCursor.getInt(0);
		countCursor.close();

		long timestamp = cursor.getLong(lastUpdateColumn);
		TextView updateTextView = ((TextView) view.findViewById(android.R.id.text2));
		updateTextView.setVisibility(View.VISIBLE);

		if (cursor.isNull(errorPosition)) {
			Date date = new Date(timestamp);

			updateTextView.setText(new StringBuilder(context.getString(R.string.update)).append(COLON).append(
					timestamp == 0 ? context.getString(R.string.never) : new StringBuilder(Constants.DATE_FORMAT.format(date)).append(' ').append(
							Constants.TIME_FORMAT.format(date))));
		} else {
			updateTextView.setText(new StringBuilder(context.getString(R.string.error)).append(COLON).append(cursor.getString(errorPosition)));
		}
		if (unreadCount > 0) {
			textView.setEnabled(true);
			updateTextView.setEnabled(true);
		} else {
			textView.setEnabled(false);
			updateTextView.setEnabled(false);
		}

		byte[] iconBytes = cursor.getBlob(iconPosition);

		if (iconBytes != null && iconBytes.length > 0) {
			Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);

			if (bitmap != null && bitmap.getHeight() > 0 && bitmap.getWidth() > 0) {
				int bitmapSizeInDip = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18f, context.getResources().getDisplayMetrics());

				if (bitmap.getHeight() != bitmapSizeInDip) {
					bitmap = Bitmap.createScaledBitmap(bitmap, bitmapSizeInDip, bitmapSizeInDip, false);
				}
				textView.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(context.getResources(), bitmap), null, null, null);
			} else {
				textView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
			}
		} else {
			view.setTag(null);
			textView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
		}
		textView.setText((cursor.isNull(namePosition) ? cursor.getString(linkPosition) : cursor.getString(namePosition))
				+ (unreadCount > 0 ? " (" + unreadCount + ")" : ""));

		View sortView = view.findViewById(R.id.sortitem);
		if (!sortViews.contains(sortView)) { // as we are reusing views, this is fine
			sortViews.add(sortView);
		}
		sortView.setVisibility(feedSort ? View.VISIBLE : View.GONE);
	}

	@Override
	protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
		ImageView indicatorImage = (ImageView) view.findViewById(R.id.indicator);

		if (cursor.getInt(isGroupPosition) == 1) {
			long feedId = cursor.getLong(idPosition);
			if (feedId == mSelectedFeedId) {
				view.setBackgroundResource(android.R.color.holo_blue_dark);
			}
			else {
				view.setBackgroundResource(android.R.color.transparent);
			}
			
			indicatorImage.setVisibility(View.VISIBLE);

			TextView textView = ((TextView) view.findViewById(android.R.id.text1));
			textView.setEnabled(true);
			textView.setText(cursor.getString(namePosition));
			textView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);

			view.findViewById(android.R.id.text2).setVisibility(View.GONE);

			View sortView = view.findViewById(R.id.sortitem);
			if (!sortViews.contains(sortView)) { // as we are reusing views, this is fine
				sortViews.add(sortView);
			}
			sortView.setVisibility(feedSort ? View.VISIBLE : View.GONE);

			final int groupPosition = cursor.getPosition();
			if (!mGroupInitDone.get(groupPosition)) {
				mGroupInitDone.put(groupPosition, true);

				boolean savedExpandedState = cursor.getInt(isGroupCollapsedPosition) == 1 ? false : true;
				if (savedExpandedState && !isExpanded) {
					mActivity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mListView.expandGroup(groupPosition);
						}
					});
				}

				if (savedExpandedState)
					indicatorImage.setImageResource(R.drawable.navigation_collapse);
				else
					indicatorImage.setImageResource(R.drawable.navigation_expand);
			} else {
				if (isExpanded)
					indicatorImage.setImageResource(R.drawable.navigation_collapse);
				else
					indicatorImage.setImageResource(R.drawable.navigation_expand);
			}
		} else {
			bindChildView(view, context, cursor);
			indicatorImage.setVisibility(View.GONE);
		}
	}

	@Override
	protected Uri getChildrenUri(Cursor groupCursor) {
		return FeedColumns.FEEDS_FOR_GROUPS_CONTENT_URI(groupCursor.getLong(idPosition));
	}

	public void startFeedActivity(long id) {
		setFeedSortEnabled(false);

		mListView.setDragNDropEnabled(false);
		mActivity.invalidateOptionsMenu();

		Intent intent = new Intent(Intent.ACTION_VIEW, EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(id));

		intent.putExtra(FeedColumns._ID, id);
		mActivity.startActivity(intent);
	}

	public void setFeedSortEnabled(boolean enabled) {
		feedSort = enabled;

		/*
		 * we do not want to call notifyDataSetChanged as this requeries the cursor
		 */
		int visibility = feedSort ? View.VISIBLE : View.GONE;

		for (View sortView : sortViews) {
			sortView.setVisibility(visibility);
		}
	}

	@Override
	public void notifyDataSetChanged() {
		reinit(null);
		super.notifyDataSetChanged();
	}

	@Override
	public void notifyDataSetChanged(Cursor data) {
		reinit(data);
	}

	@Override
	public void notifyDataSetInvalidated() {
		reinit(null);
		super.notifyDataSetInvalidated();
	}

	private void reinit(Cursor cursor) {
		sortViews.clear();

		if (cursor != null) {
			isGroupPosition = cursor.getColumnIndex(FeedColumns.IS_GROUP);
			isGroupCollapsedPosition = cursor.getColumnIndex(FeedColumns.IS_GROUP_COLLAPSED);
			namePosition = cursor.getColumnIndex(FeedColumns.NAME);
			lastUpdateColumn = cursor.getColumnIndex(FeedColumns.LAST_UPDATE);
			idPosition = cursor.getColumnIndex(FeedColumns._ID);
			linkPosition = cursor.getColumnIndex(FeedColumns.URL);
			errorPosition = cursor.getColumnIndex(FeedColumns.ERROR);
			iconPosition = cursor.getColumnIndex(FeedColumns.ICON);
		}
	}
	
    public void setSelectedFeed(long feedId) {
        mSelectedFeedId = feedId;
        mListView.invalidateViews();
    }

    public long getSelectedFeed() {
        return mSelectedFeedId;
    }
}
