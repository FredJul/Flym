package ru.yanus171.feedexfork.service;

import android.net.Uri;

public class MarkItem {
    public final String mFeedID;
    public final String mCaption;
    public final String mLink;

    public MarkItem(String feedID, String caption, String link ) {
        mFeedID = feedID;
        mCaption =  caption;
        mLink = link;
    }
}
