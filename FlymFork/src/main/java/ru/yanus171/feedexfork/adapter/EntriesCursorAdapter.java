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

package ru.yanus171.feedexfork.adapter;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.Html;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.provider.FeedData.FeedColumns;
import ru.yanus171.feedexfork.utils.NetworkUtils;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.utils.StringUtils;

public class EntriesCursorAdapter extends ResourceCursorAdapter {

    private final Uri mUri;
    private final Context mContext;
    private final boolean mShowFeedInfo;
    private final boolean mShowEntryText;
    public static final ArrayList<Uri> mMarkAsReadList = new ArrayList<Uri>();

    private int mIdPos, mTitlePos, mMainImgPos, mDatePos, mIsReadPos, mFavoritePos, mMobilizedPos, mFeedIdPos, mFeedNamePos, mAbstractPos ;

    public EntriesCursorAdapter(Context context, Uri uri, Cursor cursor, boolean showFeedInfo, boolean showEntryText) {
        super(context, R.layout.item_entry_list, cursor, 0);
        mContext = context;
        mUri = uri;
        mShowFeedInfo = showFeedInfo;
        mShowEntryText = showEntryText;
        //SetIsReadMakredList();

        mMarkAsReadList.clear();

        reinit(cursor);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return super.getView(position, convertView, parent);
    }

    public Uri EntryUri( long id ) {
        return ContentUris.withAppendedId(mUri, id);
    }
    @Override
    public void bindView(final View view, final Context context, Cursor cursor) {

        view.findViewById(R.id.text2hor).setVisibility(View.GONE);
        view.findViewById(android.R.id.text2).setVisibility(View.GONE);

        if (view.getTag(R.id.holder) == null) {
            ViewHolder holder = new ViewHolder();
            holder.titleTextView = (TextView) view.findViewById(android.R.id.text1);
            holder.textTextView = (TextView) view.findViewById(R.id.textSource);
            if ( mShowEntryText )
                holder.dateTextView = (TextView) view.findViewById(R.id.text2hor);
            else
                holder.dateTextView = (TextView) view.findViewById(android.R.id.text2);
            holder.mainImgView = (ImageView) view.findViewById(R.id.main_icon);
            holder.starImgView = (ImageView) view.findViewById(R.id.favorite_icon);
            holder.mobilizedImgView = (ImageView) view.findViewById(R.id.mobilized_icon);
            holder.readImgView = (ImageView) view.findViewById(R.id.read_icon);
            holder.textLayout = (LinearLayout)view.findViewById(R.id.textLayout);

            view.setTag(R.id.holder, holder);
        }

        final ViewHolder holder = (ViewHolder) view.getTag(R.id.holder);



        holder.dateTextView.setVisibility(View.VISIBLE);

        String titleText = cursor.getString(mTitlePos);
        holder.titleTextView.setText(titleText);
        holder.titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18 + PrefUtils.getFontSizeEntryList() );

        final long feedId = cursor.getLong(mFeedIdPos);
        final long entryID = cursor.getLong(mIdPos);

        /*if ( mShowEntryText && ( holder.entryID != -1 ) && !holder.isRead ) {
            SetIsRead(holder.entryID, true, 0);
            synchronized ( mMarkAsReadList ) {
                if ( mMarkAsReadList.contains( holder.entryID ) )
                    mMarkAsReadList.remove( holder.entryID );
            }
        }*/
        holder.entryID = entryID;


        String feedName = cursor.getString(mFeedNamePos);

        String mainImgUrl = cursor.getString(mMainImgPos);
        mainImgUrl = TextUtils.isEmpty(mainImgUrl) ? null : NetworkUtils.getDownloadedOrDistantImageUrl(entryID, mainImgUrl);

        ColorGenerator generator = ColorGenerator.DEFAULT;
        int color = generator.getColor(feedId); // The color is specific to the feedId (which shouldn't change)
        String lettersForName = feedName != null ? (feedName.length() < 2 ? feedName.toUpperCase() : feedName.substring(0, 2).toUpperCase()) : "";
        TextDrawable letterDrawable = TextDrawable.builder().buildRect(lettersForName, color);
        if (mainImgUrl != null) {
            Glide.with(context).load(mainImgUrl).centerCrop().placeholder(letterDrawable).error(letterDrawable).into(holder.mainImgView);
        } else {
            Glide.clear(holder.mainImgView);
            holder.mainImgView.setImageDrawable(letterDrawable);
        }

        holder.isFavorite = cursor.getInt(mFavoritePos) == 1;

        holder.isMobilized = !cursor.isNull(mMobilizedPos);

        if (mShowFeedInfo && mFeedNamePos > -1) {
            if (feedName != null) {
                holder.dateTextView.setText(Html.fromHtml("<font color='#247ab0'>" + feedName + "</font>" + Constants.COMMA_SPACE + StringUtils.getDateTimeString(cursor.getLong(mDatePos))));
            } else {
                holder.dateTextView.setText(StringUtils.getDateTimeString(cursor.getLong(mDatePos)));
            }
        } else {
            holder.dateTextView.setText(StringUtils.getDateTimeString(cursor.getLong(mDatePos)));
        }

        if (cursor.isNull(mIsReadPos)) {
            holder.titleTextView.setEnabled(true);
            holder.dateTextView.setEnabled(true);
            holder.isRead = false;
        } else {
            holder.titleTextView.setEnabled(false);
            holder.dateTextView.setEnabled(false);
            holder.isRead = true;
        }

        /*synchronized ( mMarkAsReadList ) {
            if ( !holder.isRead && !mMarkAsReadList.contains(holder.entryID)) {
                mMarkAsReadList.add( EntryUri( holder.entryID ) );
                Dog.i("mMarkAsReadList.add " +  EntryUri( holder.entryID ));
            }
        }*/


        /*View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (feedId >= 0) { // should not happen, but I had a crash with this on PlayStore...
                    mContext.startActivity(new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(mUri, entryId)));
                }
            }
        };

        holder.textLayout.setOnClickListener( listener );
        //holder.textTextView.setOnClickListener( listener );
        */
        //holder.starImgView.setVisibility(holder.isFavorite ? View.VISIBLE : View.INVISIBLE);
        UpdateStarImgView(holder);
        holder.starImgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFavoriteState(entryID, view);
            }
        });

        holder.mobilizedImgView.setVisibility(holder.isMobilized ? View.VISIBLE : View.INVISIBLE);

        UpdateReadImgView(holder);
        holder.readImgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleReadState(entryID, view);
            }
        });

        if ( mShowEntryText ) {
            holder.textTextView.setVisibility(View.VISIBLE);
            holder.textTextView.setText(Html.fromHtml( cursor.getString(mAbstractPos) == null ? "" : cursor.getString(mAbstractPos).toString() ));
            if ( !holder.isRead ) {
                //SetIsRead(entryId, true, 100 * 1000);
                /*if ( entryID != -1 ){
                    synchronized ( mMarkAsReadList ) {
                        if ( !mMarkAsReadList.contains(EntryUri( entryID ))) {
                            mMarkAsReadList.add(EntryUri( entryID ));
                            Dog.d("mMarkAsReadList.add " + EntryUri( entryID ));
                        }
                    }
                }*/
                //holder.entryID = entryId;

            }
            holder.textTextView.setEnabled(!holder.isRead);
            holder.mainImgView.setVisibility(View.GONE);
        } else
            holder.textTextView.setVisibility(View.GONE);

    }


    private void UpdateStarImgView(ViewHolder holder) {
        int startID = PrefUtils.getBoolean( PrefUtils.LIGHT_THEME, false ) ? R.drawable.star_gray_solid : R.drawable.star_yellow;
        holder.starImgView.setImageResource(holder.isFavorite ? startID : R.drawable.star_empty_gray );
    }
    private void UpdateReadImgView(ViewHolder holder) {
        holder.readImgView.setVisibility( mShowEntryText ? View.GONE : View.VISIBLE );
        if ( !mShowEntryText )
            holder.readImgView.setImageResource(holder.isRead ? R.drawable.rounded_checbox_gray : R.drawable.rounded_empty_gray);
    }

    public void toggleReadState(final long id, View view) {
        final ViewHolder holder = (ViewHolder) view.getTag(R.id.holder);

        if (holder != null) { // should not happen, but I had a crash with this on PlayStore...
            holder.isRead = !holder.isRead;

            if (holder.isRead) {
                holder.titleTextView.setEnabled(false);
                holder.dateTextView.setEnabled(false);
            } else {
                holder.titleTextView.setEnabled(true);
                holder.dateTextView.setEnabled(true);
            }
            UpdateReadImgView( holder );
            UpdateStarImgView( holder );


            SetIsRead(EntryUri(id), holder.isRead, 0);
        }
    }

    static public void SetIsRead(final Uri entryUri, final boolean isRead, final int sleepMsec ) {
        new Thread() {
            @Override
            public void run() {

                try {
                    if (sleepMsec > 0)
                        sleep(sleepMsec);
                } catch( InterruptedException e ) {

                }
                ContentResolver cr = MainApplication.getContext().getContentResolver();
                //Uri entryUri = ContentUris.withAppendedId(mUri, id);
                Cursor cur = cr.query( entryUri, new String[]{ EntryColumns.IS_READ }, EntryColumns.WHERE_UNREAD, null, null );
                if ( cur.moveToFirst()  )
                    cr.update(entryUri, isRead ? FeedData.getReadContentValues() : FeedData.getUnreadContentValues(), null, null);
                cur.close();
            }
        }.start();
    }

    /*public static void SetIsReadMakredList() {
        if ( !mMarkAsReadList.isEmpty() ) {
            Dog.d("SetIsReadMakredList()");
            new MarkAsRadThread().start();
        }
    }*/


    public void toggleFavoriteState(final long id, View view) {
        final ViewHolder holder = (ViewHolder) view.getTag(R.id.holder);

        if (holder != null) { // should not happen, but I had a crash with this on PlayStore...
            holder.isFavorite = !holder.isFavorite;
            //UpdateStarImgView(holder);

            new Thread() {
                @Override
                public void run() {
                    ContentValues values = new ContentValues();
                    values.put(EntryColumns.IS_FAVORITE, holder.isFavorite ? 1 : 0);

                    ContentResolver cr = MainApplication.getContext().getContentResolver();
                    Uri entryUri = ContentUris.withAppendedId(mUri, id);
                    cr.update(entryUri, values, null, null);
                }
            }.start();


        }
    }

    @Override
    public void changeCursor(Cursor cursor) {
        //SetIsReadMakredList();
        reinit(cursor);
        super.changeCursor(cursor);
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        //SetIsReadMakredList();
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
            mIdPos = cursor.getColumnIndex(EntryColumns._ID);
            mTitlePos = cursor.getColumnIndex(EntryColumns.TITLE);
            mMainImgPos = cursor.getColumnIndex(EntryColumns.IMAGE_URL);
            mDatePos = cursor.getColumnIndex(EntryColumns.DATE);
            mIsReadPos = cursor.getColumnIndex(EntryColumns.IS_READ);
            mFavoritePos = cursor.getColumnIndex(EntryColumns.IS_FAVORITE);
            mMobilizedPos = cursor.getColumnIndex(EntryColumns.MOBILIZED_HTML);
            mAbstractPos = cursor.getColumnIndex(EntryColumns.ABSTRACT);
            mFeedNamePos = cursor.getColumnIndex(FeedColumns.NAME);
            mFeedIdPos = cursor.getColumnIndex(EntryColumns.FEED_ID);
        }

    }

    private static class ViewHolder {
        public TextView titleTextView;
        public TextView textTextView;
        public TextView dateTextView;
        public ImageView mainImgView;
        public ImageView starImgView;
        public ImageView mobilizedImgView;
        public ImageView readImgView;
        public LinearLayout textLayout;
        public boolean isRead, isFavorite, isMobilized;
        public long entryID = -1;
    }

    public int GetFirstUnReadPos() {
        for (int i = 0; i < getCount(); i++) {
            Cursor cursor = (Cursor) getItem(i);
            if ( cursor.isNull( mIsReadPos ) )
                return i;
        }
        return -1;
    }
}

/*class MarkAsRadThread extends Thread  {
    //private final Uri mFeedUri;

    @Override
    public void run() {
        synchronized (EntriesCursorAdapter.mMarkAsReadList) {
            ContentResolver cr = MainApplication.getContext().getContentResolver();
            for (Uri uri : EntriesCursorAdapter.mMarkAsReadList) {
                //Uri entryUri = ContentUris.withAppendedId(mFeedUri, id);
                cr.update(uri, FeedData.getReadContentValues(), null, null);
            }
            EntriesCursorAdapter.mMarkAsReadList.clear();
        }
    }

}*/
