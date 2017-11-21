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
 * <p/>
 * <p/>
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 * <p/>
 * Copyright (c) 2010-2012 Stefan Handschuh
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.fred.feedex.adapter;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.bumptech.glide.Glide;

import net.fred.feedex.R;
import net.fred.feedex.provider.DatabaseHelper;
import net.fred.feedex.provider.FeedData;
import net.fred.feedex.utils.StringUtils;

import static android.content.ContentValues.TAG;

public class MagazineCursorAdapter extends ResourceCursorAdapter {

    private final Uri mUri;
    private final boolean mShowFeedInfo;
    private int mTitlePos, mEntryIdsPos, mMagazineID;
    DatabaseHelper mDatabaseHelper;

    public MagazineCursorAdapter(Context context, Uri uri, Cursor cursor, boolean showFeedInfo) {
        super(context, R.layout.item_magazine_list, cursor, 0);
        mUri = uri;
        mShowFeedInfo = showFeedInfo;

        reinit(cursor);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        ImageView[] imageViews = new ImageView[5];
        if (view.getTag(R.id.holder) == null) {
            ViewHolder holder = new ViewHolder();
            holder.titleTextView = (TextView) view.findViewById(R.id.magazine_title);
            imageViews[0] = (ImageView) view.findViewById(R.id.image_1);
            imageViews[1] = (ImageView) view.findViewById(R.id.image_2);
            imageViews[2] = (ImageView) view.findViewById(R.id.image_3);
            imageViews[3] = (ImageView) view.findViewById(R.id.image_4);
            imageViews[4] = (ImageView) view.findViewById(R.id.image_5);

            view.setTag(R.id.holder, holder);
        }

        mDatabaseHelper = new DatabaseHelper(new Handler(), context);
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();

        String query = "SELECT " + FeedData.MagazineColumns.ENTRY_IDS + " FROM " + FeedData.MagazineColumns.TABLE_NAME +
                " WHERE " + FeedData.MagazineColumns._ID + " = " + cursor.getString(cursor.getColumnIndex(FeedData.MagazineColumns._ID));
        Cursor mCursor = db.rawQuery(query, null);
        Cursor uCursor = null;

        if (mCursor.moveToFirst()){
            StringBuilder sb = new StringBuilder();
            String s = mCursor.getString(0);

            String[] columns = s.split(",");
            for (int i = 0; i < columns.length; i++){
                if (i != columns.length-1)
                    sb.append("'"+columns[i]+"',");
                else
                    sb.append("'"+columns[i]+"'");
            }

            uCursor = db.rawQuery("SELECT " + FeedData.EntryColumns.IMAGE_URL + " FROM " + FeedData.EntryColumns.TABLE_NAME + " WHERE " +
                    FeedData.EntryColumns._ID + " IN ( " + sb.toString() + ")",null);


        }

        int k = 0;
        while (uCursor.moveToNext()){
            if (k < imageViews.length) {
                Glide.with(context).load(uCursor.getString(0)).centerCrop().into(imageViews[k]);
                k++;
            }
            else
                break;
        }


        final ViewHolder holder = (ViewHolder) view.getTag(R.id.holder);
        String titleText = cursor.getString(mTitlePos);
        holder.titleTextView.setText(titleText);
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
        if (cursor != null && cursor.getCount() > 0) {
            mMagazineID = cursor.getColumnIndex(FeedData.MagazineColumns._ID);
            mTitlePos = cursor.getColumnIndex(FeedData.MagazineColumns.TITLE);
            mEntryIdsPos = cursor.getColumnIndex(FeedData.MagazineColumns.ENTRY_IDS);
        }
    }

    private static class ViewHolder {
        public TextView titleTextView;
    }
}
