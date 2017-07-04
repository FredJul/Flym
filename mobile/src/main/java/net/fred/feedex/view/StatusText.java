package net.fred.feedex.view;

import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import net.fred.feedex.service.FetcherService;
import net.fred.feedex.utils.Dog;

import java.util.LinkedHashMap;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by Admin on 03.06.2016.
 */


public class StatusText implements Observer {
    TextView mView;
    SwipeRefreshLayout.OnRefreshListener mOnRefreshListener;
    static int MaxID = 0;

    public StatusText( TextView view, Observable observable, SwipeRefreshLayout.OnRefreshListener onRefreshListener ) {
        mOnRefreshListener = onRefreshListener;
        observable.addObserver( this );
        mView = view;
        mView.setVisibility(View.GONE);
        mView.setGravity(Gravity.LEFT | Gravity.TOP);
        mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setVisibility(View.GONE);
            }
        });
        mView.setLines( 2 );
    }
    @Override
    public void update(Observable observable, Object data) {
        final String text = (String)data;
        mView.post(new Runnable() {
            @Override
            public void run() {
                if ( text.isEmpty() )
                    mView.setVisibility(View.GONE);
                else {
                    mView.setText(text);
                    mView.setVisibility(View.VISIBLE);
                }
                mOnRefreshListener.refreshSwipeProgress();
            }
        });
    }

    public static class FetcherObservable extends Observable {
        private Handler mHandler = null;
        volatile private int mBytesRecievedLast = 0;
        LinkedHashMap<Integer,String> mList = new LinkedHashMap<Integer,String>();
        private String mProgressText = "";
        private String mDBText = "";

        @Override
        public boolean hasChanged () {
            return true;
        }

        public void setHandler(Handler handler) {
            mHandler = handler;
        }
        private void UpdateText() {
            if ( mHandler != null )
                mHandler.postAtFrontOfQueue(new Runnable() {
                @Override
                public void run() {
                    synchronized ( mList ) {
                        String s = "";
                        //final int cRowcount = 3;
                        for( java.util.Map.Entry<Integer,String> item: mList.entrySet() ) {
                            //int index = mList.size() - i - 1;
                            //if ( index >= 0 ) {
                                //String item = mList.get(index);
                                s += item.getValue();
                                s += " ";
                            //}
                        }
                        //if ( mList.size() > cRowcount )
                        //s = "... " + s;
                        if ( !mProgressText.isEmpty() )
                            s += " " + mProgressText;
                        if ( !mList.isEmpty() && !mDBText.isEmpty() )
                            s += " " + mDBText;
                        if ( !mList.isEmpty() && FetcherService.mCancelRefresh )
                            s += "\n cancel Refresh";
                        if ( mBytesRecievedLast > 0 )
                            s = String.format( "(%.2f MB) ", ( float ) mBytesRecievedLast / 1024 / 1024 ) + s;
                        notifyObservers(s);
                        Dog.v("Status Update " + s.replace("\n", " "));
                    }
                }
            });
        }
        public int Start( final String text ) {
            Dog.v("Status Start " + text);
            synchronized ( mList ) {
                if ( mList.isEmpty() )
                    mBytesRecievedLast = 0;
                MaxID++;
                mList.put(MaxID, text );
            }
            UpdateText();
            return MaxID;
        }
        public void End( int id ) {
            Dog.v( "Status End " );
            synchronized ( mList ) {
                mProgressText = "";
                mList.remove( id );
            }
            UpdateText();
        }

        public void ChangeProgress(String text) {
            synchronized ( mList ) {
                mProgressText = text;
            }
            UpdateText();
        }
        public void ChangeDB(String text) {
            synchronized ( mList ) {
                mDBText = text;
            }
            UpdateText();
        }
        public void AddBytes(int bytes) {
            //synchronized ( mList ) {
                mBytesRecievedLast += bytes;
            //}
        }
        public void Hide() {
            if ( mHandler != null )
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (mList) {
                            if ( mList.isEmpty()  )
                                notifyObservers("");
                        }
                    }
                });
        }
    }
}

