package ru.yanus171.feedexfork.view;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Observable;
import java.util.Observer;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.service.FetcherService;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.PrefUtils;

import static ru.yanus171.feedexfork.MainApplication.NOTIFICATION_CHANNEL_ID;

/**
 * Created by Admin on 03.06.2016.
 */


public class StatusText implements Observer {
    private TextView mView;
    //SwipeRefreshLayout.OnRefreshListener mOnRefreshListener;
    static int MaxID = 0;

    public StatusText(final TextView view, final Observable observable /*, SwipeRefreshLayout.OnRefreshListener onRefreshListener*/ ) {
        //mOnRefreshListener = onRefreshListener;
        observable.addObserver( this );
        mView = view;
        mView.setVisibility(View.GONE);
        mView.setGravity(Gravity.LEFT | Gravity.TOP);
        mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FetcherObservable status = (FetcherObservable)observable;
                status.Clear();
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
            if ( !PrefUtils.getBoolean( PrefUtils.SHOW_PROGRESS_INFO, false ) || text.trim().isEmpty() )
                mView.setVisibility(View.GONE);
            else {
                mView.setText(text);
                mView.setVisibility(View.VISIBLE);
            }
            //mOnRefreshListener.refreshSwipeProgress();
            }
        });
    }

    public static final int NOTIFICATION_ID = 1;

    public static class FetcherObservable extends Observable {
        private Handler mHandler = null;
        volatile int mBytesRecievedLast = 0;
        LinkedHashMap<Integer,String> mList = new LinkedHashMap<Integer,String>();
        private String mProgressText = "";
        private String mErrorText = "";
        private String mDBText = "";
        private long mLastNotificationUpdateTime = ( new Date() ).getTime();

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
                        if ( PrefUtils.getBoolean( PrefUtils.SHOW_PROGRESS_INFO, false ) )
                            //s += TextUtils.join( " ", mList.entrySet() );
                            for( java.util.Map.Entry<Integer,String> item: mList.entrySet() )
                                    s += item.getValue() + " ";




                        //if ( mList.size() > cRowcount )
                        //s = "... " + s;
                        if ( mErrorText != null && !mErrorText.isEmpty() )
                            s += " " + mErrorText;
                        if ( PrefUtils.getBoolean( PrefUtils.SHOW_PROGRESS_INFO, false ) ) {
                            if (!mProgressText.isEmpty())
                                s += " " + mProgressText;
                            if (!mList.isEmpty() && !mDBText.isEmpty())
                                s += " " + mDBText;
                            if (!mList.isEmpty() && FetcherService.mCancelRefresh)
                                s += "\n cancel Refresh";
                            if (mBytesRecievedLast > 0)
                                s = String.format("(%.2f MB) ", (float) mBytesRecievedLast / 1024 / 1024) + s;
                        }
                        notifyObservers(s);
                        if ( PrefUtils.getBoolean( PrefUtils.IS_REFRESHING, false ) &&
                           ( ( new Date() ).getTime() - mLastNotificationUpdateTime  > 1000 ) ) {
                            Constants.NOTIF_MGR.notify(NOTIFICATION_ID, GetNotification(s));
                            mLastNotificationUpdateTime = ( new Date() ).getTime();
                        }
                        Dog.v("Status Update " + s.replace("\n", " "));
                    }
                }
            });
        }
        public void Clear() {
            synchronized ( mList ) {
                mList.clear();
                mProgressText = "";
                mDBText = "";
                mErrorText = "";
                mBytesRecievedLast = 0;
                //if (mList.isEmpty())
                //    mBytesRecievedLast = 0;
            }
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
        public void SetError( String text, Exception e ) {
            Dog.e( "Error", e );
            if ( e != null )
                e.printStackTrace();
            synchronized ( mList ) {
                mErrorText = mErrorText == null ? "" : text + "\n" + e.getCause() + "\n" + e.getLocalizedMessage();
            }
            UpdateText();
        }
        public void ChangeProgress(int textID) {
            ChangeProgress(MainApplication.getContext().getString( textID ));
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
        public void HideByScroll() {
            if ( mHandler != null )
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (mList) {
                            if ( mList.isEmpty() ) {
                                mBytesRecievedLast = 0;
                                notifyObservers("");
                            }
                        }
                    }
                });
        }
    }



    static public Notification GetNotification(String text ) {
        Context context = MainApplication.getContext();
        NotificationCompat.BigTextStyle bigxtstyle =
                new NotificationCompat.BigTextStyle();
        bigxtstyle.bigText(text);
        bigxtstyle.setBigContentTitle(context.getString(R.string.updating));
        android.support.v4.app.NotificationCompat.Builder builder = new NotificationCompat.Builder(MainApplication.getContext()) //
                //.setContentIntent(NULL) //
                .setSmallIcon(R.drawable.refresh) //
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher)) //
                //.setTicker("") //
                //.setWhen(System.currentTimeMillis()) //
                //.setAutoCancel(true) //
                //.setContentTitle(context.getString(R.string.update)) //
                //.setContentText(text) //
                .setStyle( bigxtstyle );

                //.setLights(0xffffffff, 0, 0)
        if (Build.VERSION.SDK_INT >= 26 )
            builder.setChannelId( NOTIFICATION_CHANNEL_ID );
        return builder.build();
    }


}

