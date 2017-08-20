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

package ru.yanus171.feedexfork.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.text.Html;
import android.text.TextUtils;
import android.util.Xml;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.yanus171.feedexfork.Constants;
import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.R;
import ru.yanus171.feedexfork.activity.HomeActivity;
import ru.yanus171.feedexfork.parser.RssAtomParser;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.provider.FeedData.EntryColumns;
import ru.yanus171.feedexfork.provider.FeedData.FeedColumns;
import ru.yanus171.feedexfork.provider.FeedData.TaskColumns;
import ru.yanus171.feedexfork.utils.ArticleTextExtractor;
import ru.yanus171.feedexfork.utils.Dog;
import ru.yanus171.feedexfork.utils.FileUtils;
import ru.yanus171.feedexfork.utils.HtmlUtils;
import ru.yanus171.feedexfork.utils.NetworkUtils;
import ru.yanus171.feedexfork.utils.PrefUtils;
import ru.yanus171.feedexfork.view.EntryView;
import ru.yanus171.feedexfork.view.StatusText;

public class FetcherService extends IntentService {

    public static final String ACTION_REFRESH_FEEDS = FeedData.PACKAGE_NAME + ".REFRESH";
    public static final String ACTION_MOBILIZE_FEEDS = FeedData.PACKAGE_NAME + ".MOBILIZE_FEEDS";
    public static final String ACTION_DOWNLOAD_IMAGES = FeedData.PACKAGE_NAME + ".DOWNLOAD_IMAGES";

    private static final int THREAD_NUMBER = 3;
    private static final int MAX_TASK_ATTEMPT = 3;

    private static final int FETCHMODE_DIRECT = 1;
    private static final int FETCHMODE_REENCODE = 2;
    public static final int FETCHMODE_EXERNAL_LINK = 3;

    private static final String CHARSET = "charset=";
    private static final String CONTENT_TYPE_TEXT_HTML = "text/html";
    private static final String HREF = "href=\"";

    private static final String HTML_BODY = "<body";
    private static final String ENCODING = "encoding=\"";

    public static Boolean mCancelRefresh = false;
    public static Boolean mIsCancelDownloadEntryImages = false;
    public static Boolean mIsDownloadImageCursorNeedsRequery = false;

    public static volatile Boolean mIsDeletingOld = false;


    /* Allow different positions of the "rel" attribute w.r.t. the "href" attribute */
    private static final Pattern FEED_LINK_PATTERN = Pattern.compile(
            "[.]*<link[^>]* ((rel=alternate|rel=\"alternate\")[^>]* href=\"[^\"]*\"|href=\"[^\"]*\"[^>]* (rel=alternate|rel=\"alternate\"))[^>]*>",
            Pattern.CASE_INSENSITIVE);
    public static int mMaxImageDownloadCount = PrefUtils.getImageDownloadCount();

    private final Handler mHandler;

    public static StatusText.FetcherObservable getObservable() {
        if ( mObservable == null ) {
            mObservable = new StatusText.FetcherObservable();
        }
        return mObservable;
    }

    private static StatusText.FetcherObservable mObservable = null;

    public FetcherService() {
        super(FetcherService.class.getSimpleName());
        HttpURLConnection.setFollowRedirects(true);
        mHandler = new Handler();
    }

    public static boolean hasMobilizationTask(long entryId) {
        Cursor cursor = MainApplication.getContext().getContentResolver().query(TaskColumns.CONTENT_URI, TaskColumns.PROJECTION_ID,
                TaskColumns.ENTRY_ID + '=' + entryId + Constants.DB_AND + TaskColumns.IMG_URL_TO_DL + Constants.DB_IS_NULL, null, null);

        boolean result = cursor.getCount() > 0;
        cursor.close();

        return result;
    }

    public static void addImagesToDownload(String entryId, ArrayList<String> images) {
        if (images != null && !images.isEmpty()) {
            ContentValues[] values = new ContentValues[images.size()];
            for (int i = 0; i < images.size(); i++) {
                values[i] = new ContentValues();
                values[i].put(TaskColumns.ENTRY_ID, entryId);
                values[i].put(TaskColumns.IMG_URL_TO_DL, images.get(i));
            }

            MainApplication.getContext().getContentResolver().bulkInsert(TaskColumns.CONTENT_URI, values);
        }
    }

    public static void addEntriesToMobilize(Long[] entriesId) {
        ContentValues[] values = new ContentValues[entriesId.length];
        for (int i = 0; i < entriesId.length; i++) {
            values[i] = new ContentValues();
            values[i].put(TaskColumns.ENTRY_ID, entriesId[i]);
        }

        MainApplication.getContext().getContentResolver().bulkInsert(TaskColumns.CONTENT_URI, values);
    }

    @Override
    public void onHandleIntent(Intent intent) {
        if (intent == null) { // No intent, we quit
            return;
        }

        boolean isFromAutoRefresh = intent.getBooleanExtra(Constants.FROM_AUTO_REFRESH, false);

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        // Connectivity issue, we quit
        if (networkInfo == null || networkInfo.getState() != NetworkInfo.State.CONNECTED) {
            if (ACTION_REFRESH_FEEDS.equals(intent.getAction()) && !isFromAutoRefresh) {
                // Display a toast in that case
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(FetcherService.this, R.string.network_error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return;
        }

        boolean skipFetch = isFromAutoRefresh && PrefUtils.getBoolean(PrefUtils.REFRESH_WIFI_ONLY, false)
                && networkInfo.getType() != ConnectivityManager.TYPE_WIFI;
        // We need to skip the fetching process, so we quit
        if (skipFetch) {
            return;
        }

        if (ACTION_MOBILIZE_FEEDS.equals(intent.getAction())) {
            mobilizeAllEntries();
            downloadAllImages();
        } else if (ACTION_DOWNLOAD_IMAGES.equals(intent.getAction())) {
            downloadAllImages();
        } else { // == Constants.ACTION_REFRESH_FEEDS

            startForeground(StatusText.NOTIFICATION_ID, StatusText.GetNotification( "" ) );

            int status = getObservable().Start(getString(R.string.RefreshFeeds) + ": "); try {

                PrefUtils.putBoolean(PrefUtils.IS_REFRESHING, true);
                mCancelRefresh = false;

                long keepTime = Long.parseLong(PrefUtils.getString(PrefUtils.KEEP_TIME, "4")) * 86400000l;
                long keepDateBorderTime = keepTime > 0 ? System.currentTimeMillis() - keepTime : 0;

                deleteOldEntries(keepDateBorderTime);

                String feedId = intent.getStringExtra(Constants.FEED_ID);
                String groupId = intent.getStringExtra(Constants.GROUP_ID);
                int newCount = (feedId == null ? refreshFeeds(keepDateBorderTime, groupId) : refreshFeed(feedId, keepDateBorderTime));

                if (newCount > 0) {
                    if (PrefUtils.getBoolean(PrefUtils.NOTIFICATIONS_ENABLED, true)) {
                        Cursor cursor = getContentResolver().query(EntryColumns.CONTENT_URI, new String[]{Constants.DB_COUNT}, EntryColumns.WHERE_UNREAD, null, null);

                        cursor.moveToFirst();
                        newCount = cursor.getInt(0); // The number has possibly changed
                        cursor.close();

                        if (newCount > 0) {
                            String text = getResources().getQuantityString(R.plurals.number_of_new_entries, newCount, newCount);

                            Intent notificationIntent = new Intent(FetcherService.this, HomeActivity.class);
                            PendingIntent contentIntent = PendingIntent.getActivity(FetcherService.this, 0, notificationIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT);

                            Notification.Builder notifBuilder = new Notification.Builder(MainApplication.getContext()) //
                                    .setContentIntent(contentIntent) //
                                    .setSmallIcon(R.drawable.ic_statusbar_rss) //
                                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher)) //
                                    .setTicker(text) //
                                    .setWhen(System.currentTimeMillis()) //
                                    .setAutoCancel(true) //
                                    .setContentTitle(getString(R.string.flym_feeds)) //
                                    .setContentText(text) //
                                    .setLights(0xffffffff, 0, 0);

                            if (PrefUtils.getBoolean(PrefUtils.NOTIFICATIONS_VIBRATE, false)) {
                                notifBuilder.setVibrate(new long[]{0, 1000});
                            }

                            String ringtone = PrefUtils.getString(PrefUtils.NOTIFICATIONS_RINGTONE, null);
                            if (ringtone != null && ringtone.length() > 0) {
                                notifBuilder.setSound(Uri.parse(ringtone));
                            }

                            if (PrefUtils.getBoolean(PrefUtils.NOTIFICATIONS_LIGHT, false)) {
                                notifBuilder.setLights(0xffffffff, 300, 1000);
                            }

                            if (Constants.NOTIF_MGR != null) {
                                Constants.NOTIF_MGR.notify(0, notifBuilder.getNotification());
                            }
                        }
                    } else if (Constants.NOTIF_MGR != null) {
                        Constants.NOTIF_MGR.cancel(0);
                    }
                }

                mobilizeAllEntries();
                downloadAllImages();

            } finally {
                getObservable().End( status );
            }

            PrefUtils.putBoolean(PrefUtils.IS_REFRESHING, false);

            //Constants.NOTIF_MGR.cancel( StatusText.NOTIFICATION_ID );
            stopForeground( true );

        }
        synchronized ( mCancelRefresh ) {
            mCancelRefresh = false;
        }
    }

    public static boolean isCancelRefresh() {
        synchronized (mCancelRefresh) {
            if (mCancelRefresh) {
                MainApplication.getContext().getContentResolver().delete( TaskColumns.CONTENT_URI, null, null );
            }
            return mCancelRefresh;
        }
    }

    public static boolean isCancelDownloadEntryImages() {
        synchronized (mIsCancelDownloadEntryImages) {
            return mIsCancelDownloadEntryImages;
        }
    }
    public static void setCancelDownloadEntryImages( boolean value ) {
        synchronized (mIsCancelDownloadEntryImages) {
            mIsCancelDownloadEntryImages = value;
        }
    }

    public static boolean isDownloadImageCursorNeedsRequery() {
        synchronized (mIsDownloadImageCursorNeedsRequery) {
            return mIsDownloadImageCursorNeedsRequery;
        }
    }
    public static void setDownloadImageCursorNeedsRequery( boolean value ) {
        synchronized (mIsDownloadImageCursorNeedsRequery) {
            mIsDownloadImageCursorNeedsRequery = value;
        }
    }

    private void mobilizeAllEntries() {
        int status = getObservable().Start(getString(R.string.mobilizeAll)); try {
            ContentResolver cr = getContentResolver();
            //getObservable().ChangeProgress("query DB");
            Cursor cursor = cr.query(TaskColumns.CONTENT_URI, new String[]{TaskColumns._ID, TaskColumns.ENTRY_ID, TaskColumns.NUMBER_ATTEMPT},
                    TaskColumns.IMG_URL_TO_DL + Constants.DB_IS_NULL, null, null);
            getObservable().ChangeProgress("");
            ArrayList<ContentProviderOperation> operations = new ArrayList<>();

            while (cursor.moveToNext() && !isCancelRefresh()) {
                int status1 = getObservable().Start(String.format("%d/%d", cursor.getPosition(), cursor.getCount())); try {
                    long taskId = cursor.getLong(0);
                    long entryId = cursor.getLong(1);
                    int nbAttempt = 0;
                    if (!cursor.isNull(2)) {
                        nbAttempt = cursor.getInt(2);
                    }

                    //boolean success = ;

                    if (mobilizeEntry(cr, entryId)) {
                        cr.delete(TaskColumns.CONTENT_URI(taskId), null, null);//operations.add(ContentProviderOperation.newDelete(TaskColumns.CONTENT_URI(taskId)).build());
                    } else {
                        if (nbAttempt + 1 > MAX_TASK_ATTEMPT) {
                            operations.add(ContentProviderOperation.newDelete(TaskColumns.CONTENT_URI(taskId)).build());
                        } else {
                            ContentValues values = new ContentValues();
                            values.put(TaskColumns.NUMBER_ATTEMPT, nbAttempt + 1);
                            operations.add(ContentProviderOperation.newUpdate(TaskColumns.CONTENT_URI(taskId)).withValues(values).build());
                        }
                    }

                } finally {
                    getObservable().End( status1 );
                }
            }

            cursor.close();

            if (!operations.isEmpty()) {
                getObservable().ChangeProgress(R.string.applyOperations);
                try {
                    cr.applyBatch(FeedData.AUTHORITY, operations);
                } catch (Throwable ignored) {
                }
            }
        } finally { getObservable().End( status ); }


    }

    public static boolean mobilizeEntry(ContentResolver cr, long entryId) {
        boolean success = false;

        Uri entryUri = EntryColumns.CONTENT_URI(entryId);
        Cursor entryCursor = cr.query(entryUri, null, null, null, null);

        if (entryCursor.moveToFirst()) {
            if (entryCursor.isNull(entryCursor.getColumnIndex(EntryColumns.MOBILIZED_HTML))) { // If we didn't already mobilized it
                int linkPos = entryCursor.getColumnIndex(EntryColumns.LINK);
                int abstractHtmlPos = entryCursor.getColumnIndex(EntryColumns.ABSTRACT);
                HttpURLConnection connection = null;

                try {
                    String link = entryCursor.getString(linkPos);

                    // Try to find a text indicator for better content extraction
                    String contentIndicator = null;
                    String text = entryCursor.getString(abstractHtmlPos);
                    if (!TextUtils.isEmpty(text)) {
                        text = Html.fromHtml(text).toString();
                        if (text.length() > 60) {
                            contentIndicator = text.substring(20, 40);
                        }
                    }

                    connection = NetworkUtils.setupConnection(link);

                    String mobilizedHtml = "";
                    getObservable().ChangeProgress(R.string.extractContent);

                    Document doc = Jsoup.parse(connection.getInputStream(), null, "");

                    String title = null;
                    if ( !entryCursor.isNull( entryCursor.getColumnIndex( EntryColumns.TITLE ) ) ) {
                        Elements titleEls = doc.getElementsByTag("title");
                        if (!titleEls.isEmpty())
                            title = titleEls.first().text();
                    }

                    mobilizedHtml = ArticleTextExtractor.extractContent(doc, contentIndicator);

                    getObservable().ChangeProgress("");

                    if (mobilizedHtml != null) {
                        getObservable().ChangeProgress(R.string.improveHtmlContent);
                        mobilizedHtml = HtmlUtils.improveHtmlContent(mobilizedHtml, NetworkUtils.getBaseUrl(link));
                        getObservable().ChangeProgress("");
                        ContentValues values = new ContentValues();
                        values.put(EntryColumns.MOBILIZED_HTML, mobilizedHtml);
                        if ( title != null )
                            values.put(EntryColumns.TITLE, title);

                        ArrayList<String> imgUrlsToDownload = null;
                        if (NetworkUtils.needDownloadPictures()) {
                            imgUrlsToDownload = HtmlUtils.getImageURLs(mobilizedHtml);
                        }

                        String mainImgUrl;
                        if (imgUrlsToDownload != null) {
                            mainImgUrl = HtmlUtils.getMainImageURL(imgUrlsToDownload);
                        } else {
                            mainImgUrl = HtmlUtils.getMainImageURL(mobilizedHtml);
                        }

                        if (mainImgUrl != null) {
                            values.put(EntryColumns.IMAGE_URL, mainImgUrl);
                        }

                        cr.update( entryUri, values, null, null );//operations.add(ContentProviderOperation.newUpdate(entryUri).withValues(values).build());

                        success = true;
                        if (imgUrlsToDownload != null && !imgUrlsToDownload.isEmpty()) {
                            addImagesToDownload(String.valueOf(entryId), imgUrlsToDownload);
                        }
                    }
                } catch (Throwable e) {
                    Dog.e("Mobilize error", e);
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            } else { // We already mobilized it
                success = true;
                //operations.add(ContentProviderOperation.newDelete(TaskColumns.CONTENT_URI(taskId)).build());
            }
        }
        entryCursor.close();
        return success;
    }

    public static void downloadAllImages() {
        StatusText.FetcherObservable obs = getObservable();
        int status = obs.Start(MainApplication.getContext().getString(R.string.AllImages)); try {

            ContentResolver cr = MainApplication.getContext().getContentResolver();
            Cursor cursor = cr.query(TaskColumns.CONTENT_URI, new String[]{TaskColumns._ID, TaskColumns.ENTRY_ID, TaskColumns.IMG_URL_TO_DL,
                    TaskColumns.NUMBER_ATTEMPT}, TaskColumns.IMG_URL_TO_DL + Constants.DB_IS_NOT_NULL, null, null);
            ArrayList<ContentProviderOperation> operations = new ArrayList<>();

            while (cursor.moveToNext() && !isCancelRefresh() && !isDownloadImageCursorNeedsRequery()) {
                int status1 = obs.Start(String.format("%d/%d", cursor.getPosition() + 1, cursor.getCount())); try {
                //int status1 = obs.Start(String.format("%d", cursor.getPosition() + 1, cursor.getCount())); try {
                    long taskId = cursor.getLong(0);
                    long entryId = cursor.getLong(1);
                    String imgPath = cursor.getString(2);
                    int nbAttempt = 0;
                    if (!cursor.isNull(3)) {
                        nbAttempt = cursor.getInt(3);
                    }

                    try {
                        NetworkUtils.downloadImage(entryId, imgPath/*, false*/);

                        // If we are here, everything is OK
                        operations.add(ContentProviderOperation.newDelete(TaskColumns.CONTENT_URI(taskId)).build());
                    } catch (Exception e) {
                        if (nbAttempt + 1 > MAX_TASK_ATTEMPT) {
                            operations.add(ContentProviderOperation.newDelete(TaskColumns.CONTENT_URI(taskId)).build());
                        } else {
                            ContentValues values = new ContentValues();
                            values.put(TaskColumns.NUMBER_ATTEMPT, nbAttempt + 1);
                            operations.add(ContentProviderOperation.newUpdate(TaskColumns.CONTENT_URI(taskId)).withValues(values).build());
                        }
                    }
                    EntryView.NotifyToUpdate( entryId );
                } finally {
                    obs.End( status1 );
                }

            }

            cursor.close();

            if (!operations.isEmpty()) {
                obs.ChangeProgress(R.string.applyOperations);
                try {
                    cr.applyBatch(FeedData.AUTHORITY, operations);
                } catch (Throwable ignored) {

                }
            }
        } finally { obs.End( status ); }

        if ( isDownloadImageCursorNeedsRequery() ) {
            setDownloadImageCursorNeedsRequery( false );
            downloadAllImages();
        }
    }

    public static void downloadEntryImages( long entryId, ArrayList<String> imageList ) {
        StatusText.FetcherObservable obs = getObservable();
        int status = obs.Start(MainApplication.getContext().getString(R.string.EntryImages)); try {
            for( String imgPath: imageList ) {
                if (isCancelRefresh() || isCancelDownloadEntryImages())
                    break;
                int status1 = obs.Start(String.format("%d/%d", imageList.indexOf(imgPath) + 1, imageList.size()));
                try {
                    NetworkUtils.downloadImage(entryId, imgPath/*, false*/);
                } catch (Exception e) {

                } finally {
                    obs.End(status1);
                }
            }
        } finally { obs.End( status ); }
        EntryView.NotifyToUpdate( entryId );
    }


    private void deleteOldEntries(final long keepDateBorderTime) {
        if (keepDateBorderTime > 0) {
                if ( !mIsDeletingOld )
                    new Thread() {
                        @Override
                        public void run() {
                            int status = getObservable().Start(MainApplication.getContext().getString(R.string.deleteOldEntries)); try {
                                mIsDeletingOld = true;
                                    String where = EntryColumns.DATE + '<' + keepDateBorderTime + Constants.DB_AND + EntryColumns.WHERE_NOT_FAVORITE;
                                // Delete the entries, the cache files will be deleted by the content provider
                                MainApplication.getContext().getContentResolver().delete(EntryColumns.CONTENT_URI, where, null);

                                getObservable().ChangeProgress(R.string.deleteImages);
                                File[] files = FileUtils.GetImagesFolder().listFiles(new FileFilter() {//NetworkUtils.IMAGE_FOLDER_FILE.listFiles(new FileFilter() {
                                    @Override
                                    public boolean accept(File pathname) {
                                        return (pathname.lastModified() < keepDateBorderTime);
                                                }
                                });
                                if ( files != null ) {
                                    int i = 0;
                                    for( File file: files ) {
                                        i++;
                                        getObservable().ChangeProgress(getString(R.string.deleteImages) + String.format( " %d/%d", i, files.length ) );
                                        file.delete();
                                    }
                                }
                                getObservable().ChangeProgress("");
                            } finally {
                                getObservable().End( status );
                                mIsDeletingOld = false;
                            }
                        }
                    }.start();
        }
    }

    private int refreshFeeds(final long keepDateBorderTime, String groupID) {

        ContentResolver cr = getContentResolver();
        final Cursor cursor;
        if ( groupID != null )
            cursor = cr.query(FeedColumns.FEEDS_FOR_GROUPS_CONTENT_URI(groupID), FeedColumns.PROJECTION_ID, null, null, null);
        else
            cursor = cr.query(FeedColumns.CONTENT_URI, FeedColumns.PROJECTION_ID, null, null, null);
        int nbFeed = cursor.getCount();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_NUMBER, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            }
        });

        CompletionService<Integer> completionService = new ExecutorCompletionService<>(executor);
        while (cursor.moveToNext()) {
            //getObservable().Start(String.format("%d from %d", cursor.getPosition(), cursor.getCount()));
            final String feedId = cursor.getString(0);
            completionService.submit(new Callable<Integer>() {
                @Override
                public Integer call() {
                    int result = 0;
                    try {
                        if (!isCancelRefresh())
                            result = refreshFeed(feedId, keepDateBorderTime);
                    } catch (Exception ignored) {
                    }
                    return result;
                }
            });
            //getObservable().End();
        }
        cursor.close();

        int globalResult = 0;
        for (int i = 0; i < nbFeed; i++) {
            try {
                Future<Integer> f = completionService.take();
                globalResult += f.get();
            } catch (Exception ignored) {
            }
        }

        executor.shutdownNow(); // To purge all threads

        return globalResult;
    }

    private int refreshFeed(String feedId, long keepDateBorderTime) {
        RssAtomParser handler = null;

        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(FeedColumns.CONTENT_URI(feedId), null, null, null, null);

        if (cursor.moveToFirst()) {
            int urlPosition = cursor.getColumnIndex(FeedColumns.URL);
            int idPosition = cursor.getColumnIndex(FeedColumns._ID);
            int titlePosition = cursor.getColumnIndex(FeedColumns.NAME);
            int fetchModePosition = cursor.getColumnIndex(FeedColumns.FETCH_MODE);
            int realLastUpdatePosition = cursor.getColumnIndex(FeedColumns.REAL_LAST_UPDATE);
            int iconPosition = cursor.getColumnIndex(FeedColumns.ICON);
            int retrieveFullscreenPosition = cursor.getColumnIndex(FeedColumns.RETRIEVE_FULLTEXT);
            //int showTextInEntryList = cursor.getColumnIndex(FeedColumns.SHOW_TEXT_IN_ENTRY_LIST);


            String id = cursor.getString(idPosition);
            HttpURLConnection connection = null;

            int status = getObservable().Start(cursor.getString(titlePosition));
            try {

                String feedUrl = cursor.getString(urlPosition);
                connection = NetworkUtils.setupConnection(feedUrl);
                String contentType = connection.getContentType();
                int fetchMode = cursor.getInt(fetchModePosition);

                handler = new RssAtomParser(new Date(cursor.getLong(realLastUpdatePosition)),
                                            keepDateBorderTime,
                                            id,
                                            cursor.getString(titlePosition),
                                            feedUrl,
                                            cursor.getInt(retrieveFullscreenPosition) == 1);
                handler.setFetchImages(NetworkUtils.needDownloadPictures());

                if (fetchMode == 0) {
                    if (contentType != null && contentType.startsWith(CONTENT_TYPE_TEXT_HTML)) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                        String line;
                        int posStart = -1;

                        while ((line = reader.readLine()) != null) {
                            FetcherService.getObservable().AddBytes( line.length() );
                            if (line.contains(HTML_BODY)) {
                                break;
                            } else {
                                Matcher matcher = FEED_LINK_PATTERN.matcher(line);

                                if (matcher.find()) { // not "while" as only one link is needed
                                    line = matcher.group();
                                    posStart = line.indexOf(HREF);

                                    if (posStart > -1) {
                                        String url = line.substring(posStart + 6, line.indexOf('"', posStart + 10)).replace(Constants.AMP_SG,
                                                Constants.AMP);

                                        ContentValues values = new ContentValues();

                                        if (url.startsWith(Constants.SLASH)) {
                                            int index = feedUrl.indexOf('/', 8);

                                            if (index > -1) {
                                                url = feedUrl.substring(0, index) + url;
                                            } else {
                                                url = feedUrl + url;
                                            }
                                        } else if (!url.startsWith(Constants.HTTP_SCHEME) && !url.startsWith(Constants.HTTPS_SCHEME)) {
                                            url = feedUrl + '/' + url;
                                        }
                                        values.put(FeedColumns.URL, url);
                                        cr.update(FeedColumns.CONTENT_URI(id), values, null, null);
                                        connection.disconnect();
                                        connection = NetworkUtils.setupConnection(url);
                                        contentType = connection.getContentType();
                                        break;
                                    }
                                }
                            }
                        }
                        // this indicates a badly configured feed
                        if (posStart == -1) {
                            connection.disconnect();
                            connection = NetworkUtils.setupConnection(feedUrl);
                            contentType = connection.getContentType();
                        }
                    }

                    if (contentType != null) {
                        int index = contentType.indexOf(CHARSET);

                        if (index > -1) {
                            int index2 = contentType.indexOf(';', index);

                            try {
                                Xml.findEncodingByName(index2 > -1 ? contentType.substring(index + 8, index2) : contentType.substring(index + 8));
                                fetchMode = FETCHMODE_DIRECT;
                            } catch (UnsupportedEncodingException ignored) {
                                fetchMode = FETCHMODE_REENCODE;
                            }
                        } else {
                            fetchMode = FETCHMODE_REENCODE;
                        }

                    } else {
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                        char[] chars = new char[20];

                        int length = bufferedReader.read(chars);

                        FetcherService.getObservable().AddBytes( length );

                        String xmlDescription = new String(chars, 0, length);

                        connection.disconnect();
                        connection = NetworkUtils.setupConnection(connection.getURL());

                        int start = xmlDescription.indexOf(ENCODING);

                        if (start > -1) {
                            try {
                                Xml.findEncodingByName(xmlDescription.substring(start + 10, xmlDescription.indexOf('"', start + 11)));
                                fetchMode = FETCHMODE_DIRECT;
                            } catch (UnsupportedEncodingException ignored) {
                                fetchMode = FETCHMODE_REENCODE;
                            }
                        } else {
                            // absolutely no encoding information found
                            fetchMode = FETCHMODE_DIRECT;
                        }
                    }

                    ContentValues values = new ContentValues();
                    values.put(FeedColumns.FETCH_MODE, fetchMode);
                    cr.update(FeedColumns.CONTENT_URI(id), values, null, null);
                }

                switch (fetchMode) {
                    default:
                    case FETCHMODE_DIRECT: {
                        if (contentType != null) {
                            int index = contentType.indexOf(CHARSET);

                            int index2 = contentType.indexOf(';', index);

                            InputStream inputStream = connection.getInputStream();
                            parseXml(inputStream,
                                    Xml.findEncodingByName(index2 > -1 ? contentType.substring(index + 8, index2) : contentType.substring(index + 8)),
                                    handler);

                        } else {
                            InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                            parseXml(reader, handler);

                        }
                        break;
                    }
                    case FETCHMODE_REENCODE: {
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        InputStream inputStream = connection.getInputStream();

                        byte[] byteBuffer = new byte[4096];

                        int n;
                        while ((n = inputStream.read(byteBuffer)) > 0) {
                            FetcherService.getObservable().AddBytes( n );
                            outputStream.write(byteBuffer, 0, n);
                        }

                        String xmlText = outputStream.toString();

                        int start = xmlText != null ? xmlText.indexOf(ENCODING) : -1;

                        if (start > -1) {
                            parseXml(
                                    new StringReader(new String(outputStream.toByteArray(),
                                            xmlText.substring(start + 10, xmlText.indexOf('"', start + 11)))), handler
                            );
                        } else {
                            // use content type
                            if (contentType != null) {
                                int index = contentType.indexOf(CHARSET);

                                if (index > -1) {
                                    int index2 = contentType.indexOf(';', index);

                                    try {
                                        StringReader reader = new StringReader(new String(outputStream.toByteArray(), index2 > -1 ? contentType.substring(
                                                index + 8, index2) : contentType.substring(index + 8)));
                                        parseXml(reader, handler);
                                    } catch (Exception ignored) {
                                    }
                                } else {
                                    StringReader reader = new StringReader(new String(outputStream.toByteArray()));
                                    parseXml(reader, handler);
                                }
                            }
                        }
                        break;
                    }
                }

                connection.disconnect();
            } catch (FileNotFoundException e) {
                if (handler == null || (!handler.isDone() && !handler.isCancelled())) {
                    ContentValues values = new ContentValues();

                    // resets the fetch mode to determine it again later
                    values.put(FeedColumns.FETCH_MODE, 0);

                    values.put(FeedColumns.ERROR, getString(R.string.error_feed_error));
                    cr.update(FeedColumns.CONTENT_URI(id), values, null, null);
                }
            } catch (Throwable e) {
                if (handler == null || (!handler.isDone() && !handler.isCancelled())) {
                    ContentValues values = new ContentValues();

                    // resets the fetch mode to determine it again later
                    values.put(FeedColumns.FETCH_MODE, 0);

                    values.put(FeedColumns.ERROR, e.getMessage() != null ? e.getMessage() : getString(R.string.error_feed_process));
                    cr.update(FeedColumns.CONTENT_URI(id), values, null, null);
                }
            } finally {

				/* check and optionally find favicon */
                try {
                    if (handler != null && cursor.getBlob(iconPosition) == null) {
                        String feedLink = handler.getFeedLink();
                        if (feedLink != null) {
                            NetworkUtils.retrieveFavicon(this, new URL(feedLink), id);
                        } else {
                            NetworkUtils.retrieveFavicon(this, connection.getURL(), id);
                        }
                    }
                } catch (Throwable ignored) {
                }

                if (connection != null) {
                    connection.disconnect();
                }
                getObservable().End( status );
            }
        }

        cursor.close();

        return handler != null ? handler.getNewCount() : 0;
    }

    private static void parseXml(InputStream in, Xml.Encoding encoding,
                             ContentHandler contentHandler) throws IOException, SAXException {
        getObservable().ChangeProgress( R.string.parseXml );
        Xml.parse(in, encoding, contentHandler);
        getObservable().ChangeProgress( "" );
        getObservable().AddBytes(contentHandler.toString().length());
    }
    private static void parseXml(Reader reader,
                                 ContentHandler contentHandler) throws IOException, SAXException {
        getObservable().ChangeProgress( R.string.parseXml );
        Xml.parse(reader, contentHandler);
        getObservable().ChangeProgress( "" );
        getObservable().AddBytes(contentHandler.toString().length() );
    }

    public static void cancelRefresh() {
    //if (PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
        synchronized (mCancelRefresh) {
            mCancelRefresh = true;
        }
    //}
    }

    public static void deleteAllFeedEntries( String feedID ) {
        int status = getObservable().Start("deleteAllFeedEntries");
        try {
            ContentResolver cr = MainApplication.getContext().getContentResolver();
            cr.delete(EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(feedID), EntryColumns.WHERE_NOT_FAVORITE, null);
            ContentValues values = new ContentValues();
            values.putNull( FeedColumns.LAST_UPDATE );
            values.putNull( FeedColumns.REAL_LAST_UPDATE );
            cr.update(FeedColumns.CONTENT_URI( feedID ), values, null, null );
        } finally {
            getObservable().End(status);
        }

    }

    public static void createTestData() {
        int status = getObservable().Start("createTestData");
        try {
            final String testFeedID = "10000";
            final String testAbstract1 = "safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd safdkhfgsadjkhgfsakdhgfasdhkgf sadfdasfdsafasdfasd ";
            String testAbstract = "";
            for ( int i = 0; i < 10; i++  )
                testAbstract += testAbstract1;
            //final String testAbstract2 = "sfdsdafsdafs sfdsdafsdafs sfdsdafsdafs sfdsdafsdafs sfdsdafsdafs sfdsdafsdafs sfdsdafsdafs sfdsdafsdafs sfdsdafsdafs fffffffffffffff fffffffffffffff fffffffffffffff fffffffffffffff fffffffffffffff fffffffffffffff fffffffffffffff fffffffffffffff";

            deleteAllFeedEntries(testFeedID);

            ContentResolver cr = MainApplication.getContext().getContentResolver();
            ContentValues values = new ContentValues();
            values.put(FeedColumns._ID, testFeedID);
            values.put(FeedColumns.NAME, "testFeed");
            values.putNull(FeedColumns.IS_GROUP);
            //values.putNull(FeedColumns.GROUP_ID);
            values.putNull(FeedColumns.LAST_UPDATE);
            values.put(FeedColumns.FETCH_MODE, 0);
            cr.insert(FeedColumns.CONTENT_URI, values);

            for( int i = 0; i < 30; i++ ) {
                values.clear();
                values.put(EntryColumns._ID, i);
                values.put(EntryColumns.ABSTRACT, testAbstract);
                values.put(EntryColumns.TITLE, "testTitile" + i);
                cr.insert(EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(testFeedID), values);
            }
        } finally {
            getObservable().End(status);
        }

    }
}
