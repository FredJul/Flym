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

package net.fred.feedex.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import net.fred.feedex.R;
import net.fred.feedex.provider.FeedData.FilterColumns;

public class FiltersCursorAdapter extends ResourceCursorAdapter {

    private int mFilterTextColumnPosition;
    private int mIsAppliedToTitleColumnPosition;
    private int mIsAcceptRulePosition;

    private int mSelectedFilter = -1;

    public FiltersCursorAdapter(Context context, Cursor cursor) {
        super(context, R.layout.item_rule_list, cursor, 0);

        reinit(cursor);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        TextView isAcceptRuleTextView = (TextView) view.findViewById(android.R.id.text1);
        TextView filterTextTextView = (TextView) view.findViewById(android.R.id.text2);
        TextView isAppliedToTitleTextView = (TextView) view.findViewById(R.id.text3);

        if (cursor.getPosition() == mSelectedFilter) {
            view.setBackgroundResource(android.R.color.holo_blue_dark);
        } else {
            view.setBackgroundResource(android.R.color.transparent);
        }

        boolean isAcceptRule = cursor.getInt(mIsAcceptRulePosition) == 1;
        isAcceptRuleTextView.setText(isAcceptRule ? R.string.accept : R.string.reject);
        isAcceptRuleTextView.setTextColor(isAcceptRule ? context.getResources().getColor(android.R.color.holo_green_dark) :
                context.getResources().getColor(android.R.color.holo_red_dark));
        filterTextTextView.setText(cursor.getString(mFilterTextColumnPosition));
        isAppliedToTitleTextView.setText(cursor.getInt(mIsAppliedToTitleColumnPosition) == 1 ? R.string.filter_apply_to_title : R.string.filter_apply_to_content);
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
            mFilterTextColumnPosition = cursor.getColumnIndex(FilterColumns.FILTER_TEXT);
            mIsAppliedToTitleColumnPosition = cursor.getColumnIndex(FilterColumns.IS_APPLIED_TO_TITLE);
            mIsAcceptRulePosition = cursor.getColumnIndex(FilterColumns.IS_ACCEPT_RULE);
        }
    }

    public int getSelectedFilter() {
        return mSelectedFilter;
    }

    public void setSelectedFilter(int filterPos) {
        mSelectedFilter = filterPos;
    }
}
