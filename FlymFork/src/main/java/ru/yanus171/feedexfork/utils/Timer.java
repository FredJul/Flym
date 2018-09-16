package ru.yanus171.feedexfork.utils;

import android.util.Log;

import java.util.Date;
import java.util.HashMap;

public class Timer {
    private final String mName;
    private final Date mStart;
    private static final HashMap<Integer, Timer> mVoc = new HashMap<>();

    public Timer(String name ){
        mName = name;
        mStart = new Date();
    }
    public void End() {
        Log.d( "FFTimer", String.format( "%d msec: %s", new Date().getTime() - mStart.getTime(), mName ) );
    }

    public static void End( int id ) {
        if ( mVoc.containsKey( id ) ) {
            mVoc.get(id).End();
            mVoc.remove(id);
        }
    }

    public static void Start( int id, String name ){
        if ( mVoc.containsKey( id ) )
            mVoc.remove( id );
        mVoc.put( id, new Timer( name ) );
    }


}
