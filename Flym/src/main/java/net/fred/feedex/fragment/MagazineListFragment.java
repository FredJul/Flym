package net.fred.feedex.fragment;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import net.fred.feedex.MainApplication;
import net.fred.feedex.R;
import net.fred.feedex.adapter.MagazineCursorAdapter;
import net.fred.feedex.provider.FeedData;

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
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Cursor magazineItem = (Cursor) mMagazineCursorAdapter.getItem(i);
                String magazineId = magazineItem.getString(magazineItem.getColumnIndex(FeedData.MagazineColumns._ID));

//                ContentResolver cr = MainApplication.getContext().getContentResolver();
//                String [] requestedColumns = {
//                        FeedData.MagazineColumns.ENTRY_IDS,
//                };
//                Cursor entry = cr.query(FeedData.MagazineColumns.CONTENT_URI,
//                        requestedColumns,
//                        FeedData.MagazineColumns._ID + "=" + magazineId + "",
//                        null, null);
//
                String entryList = "7";
//
//                if(entry != null) {
//                    if (entry.moveToFirst()) {
//                        entryList = entry.getString(entry.getColumnIndex(FeedData.MagazineColumns.ENTRY_IDS)).replace(',', '.');
//                    }
//                }

                EntriesListFragment mEntriesListFragment = new EntriesListFragment();
                mEntriesListFragment.setDataValues(FeedData.EntryColumns.ENTRIES_FOR_MAGAZINE_URI(Long.parseLong(magazineId)), false);
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.home_fragment_frame, mEntriesListFragment).commit();


                //entry.close();
//                magazineItem.close();
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        String a = "";
    }
}
