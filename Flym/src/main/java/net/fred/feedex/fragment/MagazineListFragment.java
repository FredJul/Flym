package net.fred.feedex.fragment;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.fred.feedex.Constants;
import net.fred.feedex.R;
import net.fred.feedex.adapter.MagazineCursorAdapter;
import net.fred.feedex.provider.FeedData;
import net.fred.feedex.provider.FeedDataContentProvider;

/**
 * A simple {@link Fragment} subclass.
 */
public class MagazineListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private MagazineCursorAdapter mMagazineCursorAdapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getLoaderManager().initLoader(0, null, this);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_magazine, container, false);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = new CursorLoader(this.getContext(),
                FeedData.MagazineColumns.CONTENT_URI, null, null, null, null);

        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (mMagazineCursorAdapter == null) {
            mMagazineCursorAdapter = new MagazineCursorAdapter(getActivity(), FeedData.MagazineColumns.CONTENT_URI, data, false);

        }
        else{
            mMagazineCursorAdapter.swapCursor(data);
        }
        setListAdapter(mMagazineCursorAdapter);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
