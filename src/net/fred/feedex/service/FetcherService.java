/**
 * FeedEx
 * 
 * Copyright (c) 2012-2013 Frederic Julian
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 	
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 * 
 * Copyright (c) 2010-2012 Stefan Handschuh
 * 
 *     Permission is hereby granted, free of charge, to any person obtaining a copy
 *     of this software and associated documentation files (the "Software"), to deal
 *     in the Software without restriction, including without limitation the rights
 *     to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *     copies of the Software, and to permit persons to whom the Software is
 *     furnished to do so, subject to the following conditions:
 * 
 *     The above copyright notice and this permission notice shall be included in
 *     all copies or substantial portions of the Software.
 * 
 *     THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *     IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *     FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *     AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *     LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *     OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *     THE SOFTWARE.
 */

package net.fred.feedex.service;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
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
import java.util.zip.GZIPInputStream;

import net.fred.feedex.Constants;
import net.fred.feedex.MainApplication;
import net.fred.feedex.PrefsManager;
import net.fred.feedex.R;
import net.fred.feedex.activity.MainActivity;
import net.fred.feedex.handler.RssAtomHandler;
import net.fred.feedex.provider.FeedData.EntryColumns;
import net.fred.feedex.provider.FeedData.FeedColumns;
import net.fred.feedex.provider.FeedDataContentProvider;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.Html;
import android.util.Base64;
import android.util.Xml;

public class FetcherService extends IntentService {

	private static final int THREAD_NUMBER = 3;

	private static final String MOBILIZER_URL = "http://ftr.fivefilters.org/makefulltextfeed.php?url=";

	private static final int FETCHMODE_DIRECT = 1;
	private static final int FETCHMODE_REENCODE = 2;

	private static final String KEY_USERAGENT = "User-agent";
	private static final String VALUE_USERAGENT = "Mozilla/5.0";
	private static final String CHARSET = "charset=";
	private static final String COUNT = "COUNT(*)";
	private static final String CONTENT_TYPE_TEXT_HTML = "text/html";
	private static final String HREF = "href=\"";
	private static final String HTML_BODY = "<body";
	private static final String ENCODING = "encoding=\"";
	private static final String SERVICENAME = "RssFetcherService";
	private static final String ZERO = "0";
	private static final String GZIP = "gzip";
	private static final String FILE_FAVICON = "/favicon.ico";
	private static final String DEFAULT_PROXY_PORT = "8080";
	private static final String PROTOCOL_SEPARATOR = "://";
	private static final String _HTTP = "http";
	private static final String _HTTPS = "https";
	/* Allow different positions of the "rel" attribute w.r.t. the "href" attribute */
	private static final Pattern feedLinkPattern = Pattern
			.compile("[.]*<link[^>]* ((rel=alternate|rel=\"alternate\")[^>]* href=\"[^\"]*\"|href=\"[^\"]*\"[^>]* (rel=alternate|rel=\"alternate\"))[^>]*>");

	private NotificationManager notificationManager;
	private static Proxy proxy;

	public FetcherService() {
		super(SERVICENAME);
		HttpURLConnection.setFollowRedirects(true);
	}

	@Override
	public void onHandleIntent(Intent intent) {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

		if (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED && intent != null) {
			if (intent.getBooleanExtra(Constants.SCHEDULED, false)) {
				PrefsManager.putLong(PrefsManager.LAST_SCHEDULED_REFRESH, SystemClock.elapsedRealtime());
			}

			if (PrefsManager.getBoolean(PrefsManager.PROXY_ENABLED, false)
					&& (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || !PrefsManager.getBoolean(PrefsManager.PROXY_WIFI_ONLY, false))) {
				try {
					proxy = new Proxy(ZERO.equals(PrefsManager.getString(PrefsManager.PROXY_TYPE, ZERO)) ? Proxy.Type.HTTP : Proxy.Type.SOCKS,
							new InetSocketAddress(PrefsManager.getString(PrefsManager.PROXY_HOST, ""), Integer.parseInt(PrefsManager.getString(
									PrefsManager.PROXY_PORT, DEFAULT_PROXY_PORT))));
				} catch (Exception e) {
					proxy = null;
				}
			} else {
				proxy = null;
			}

			if (intent.hasExtra(Constants.ENTRY_URI)) {
				mobilizeFeed((Uri) intent.getParcelableExtra(Constants.ENTRY_URI));
			} else {
				String feedId = intent.getStringExtra(Constants.FEED_ID);
				int newCount = (feedId == null ? refreshFeeds() : refreshFeed(feedId));

				if (newCount > 0) {
					if (PrefsManager.getBoolean(PrefsManager.NOTIFICATIONS_ENABLED, true)) {
						Cursor cursor = getContentResolver().query(EntryColumns.CONTENT_URI, new String[] { COUNT }, EntryColumns.WHERE_UNREAD, null,
								null);

						cursor.moveToFirst();
						newCount = cursor.getInt(0); // The number has possibly changed
						cursor.close();

						if (newCount > 0) {
							String text = new StringBuilder().append(newCount).append(' ').append(getString(R.string.new_entries)).toString();

							Intent notificationIntent = new Intent(FetcherService.this, MainActivity.class);
							PendingIntent contentIntent = PendingIntent.getActivity(FetcherService.this, 0, notificationIntent,
									PendingIntent.FLAG_UPDATE_CURRENT);

							Notification.Builder notifBuilder = new Notification.Builder(MainApplication.getAppContext()) //
									.setContentIntent(contentIntent) //
									.setSmallIcon(R.drawable.ic_statusbar_rss) //
									.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon)) //
									.setTicker(text) //
									.setWhen(System.currentTimeMillis()) //
									.setAutoCancel(true) //
									.setContentTitle(getString(R.string.feedex_feeds)) //
									.setContentText(text) //
									.setLights(0xffffffff, 300, 1000);

							if (PrefsManager.getBoolean(PrefsManager.NOTIFICATIONS_VIBRATE, false)) {
								notifBuilder.setVibrate(new long[] { 0, 1000 });
							}

							String ringtone = PrefsManager.getString(PrefsManager.NOTIFICATIONS_RINGTONE, null);
							if (ringtone != null && ringtone.length() > 0) {
								notifBuilder.setSound(Uri.parse(ringtone));
							}

							notificationManager.notify(0, notifBuilder.getNotification());
						}
					} else {
						notificationManager.cancel(0);
					}
				}

				sendBroadcast(new Intent(Constants.ACTION_REFRESH_FINISHED));
			}
		} else {
			sendBroadcast(new Intent(Constants.ACTION_REFRESH_FINISHED));
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	private void mobilizeFeed(Uri uri) {
		ContentResolver cr = getContentResolver();
		Cursor entryCursor = cr.query(uri, null, null, null, null);

		if (entryCursor.moveToNext()) {
			HttpURLConnection connection = null;
			int linkPosition = entryCursor.getColumnIndex(EntryColumns.LINK);

			try {
				String link = entryCursor.getString(linkPosition);
				connection = setupConnection(MOBILIZER_URL + link);
				BufferedReader reader = new BufferedReader(new InputStreamReader(getConnectionInputStream(connection)));

				StringBuilder sb = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}

				String mobilizedHtml = null;
				Pattern p = Pattern.compile("<description>[^<]*</description>.*<description>(.*)&lt;p&gt;&lt;em&gt;This entry passed through the");
				Matcher m = p.matcher(sb.toString());
				if (m.find()) {
					mobilizedHtml = m.toMatchResult().group(1);
				} else {
					p = Pattern.compile("<description>[^<]*</description>.*<description>(.*)</description>");
					m = p.matcher(sb.toString());
					if (m.find()) {
						mobilizedHtml = m.toMatchResult().group(1);
					}
				}

				if (mobilizedHtml != null) {
					ContentValues values = new ContentValues();
					values.put(EntryColumns.MOBILIZED_HTML, Html.fromHtml(mobilizedHtml, null, null).toString());
					cr.update(uri, values, null, null);
				}
			} catch (Throwable e) {
				e.printStackTrace();
			} finally {
				if (connection != null) {
					connection.disconnect();
				}
			}
		}
		entryCursor.close();
	}

	private int refreshFeeds() {
		ContentResolver cr = getContentResolver();
		final Cursor cursor = cr.query(FeedColumns.CONTENT_URI, FeedColumns.PROJECTION_ID, null, null, null);
		int nbFeed = cursor.getCount();

		ExecutorService executor = Executors.newFixedThreadPool(THREAD_NUMBER, new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setPriority(Thread.MIN_PRIORITY);
				return t;
			}
		});

		CompletionService<Integer> completionService = new ExecutorCompletionService<Integer>(executor);
		while (cursor.moveToNext()) {
			final String feedId = cursor.getString(0);
			completionService.submit(new Callable<Integer>() {
				@Override
				public Integer call() {
					int result = 0;
					try {
						result = refreshFeed(feedId);
					} catch (Exception ex) {
					}
					return result;
				}
			});
		}
		cursor.close();

		int globalResult = 0;
		for (int i = 0; i < nbFeed; i++) {
			try {
				Future<Integer> f = completionService.take();
				globalResult += f.get();
			} catch (Exception e) {
			}
		}

		executor.shutdownNow(); // To purge all threads

		return globalResult;
	}

	private int refreshFeed(String feedId) {
		RssAtomHandler handler = null;

		ContentResolver cr = getContentResolver();
		Cursor cursor = cr.query(FeedColumns.CONTENT_URI(feedId), null, null, null, null);

		if (cursor.moveToFirst()) {
			int urlPosition = cursor.getColumnIndex(FeedColumns.URL);
			int idPosition = cursor.getColumnIndex(FeedColumns._ID);
			int titlePosition = cursor.getColumnIndex(FeedColumns.NAME);
			int fetchmodePosition = cursor.getColumnIndex(FeedColumns.FETCH_MODE);
			int realLastUpdatePosition = cursor.getColumnIndex(FeedColumns.REAL_LAST_UPDATE);
			int iconPosition = cursor.getColumnIndex(FeedColumns.ICON);

			String id = cursor.getString(idPosition);
			HttpURLConnection connection = null;

			try {
				String feedUrl = cursor.getString(urlPosition);
				connection = setupConnection(feedUrl);
				String contentType = connection.getContentType();
				int fetchMode = cursor.getInt(fetchmodePosition);

				handler = new RssAtomHandler(new Date(cursor.getLong(realLastUpdatePosition)), id, cursor.getString(titlePosition), feedUrl);
				handler.setFetchImages(PrefsManager.getBoolean(PrefsManager.FETCH_PICTURES, false));

				if (fetchMode == 0) {
					if (contentType != null && contentType.startsWith(CONTENT_TYPE_TEXT_HTML)) {
						BufferedReader reader = new BufferedReader(new InputStreamReader(getConnectionInputStream(connection)));

						String line = null;
						int posStart = -1;

						while ((line = reader.readLine()) != null) {
							if (line.indexOf(HTML_BODY) > -1) {
								break;
							} else {
								Matcher matcher = feedLinkPattern.matcher(line);

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
										} else if (!url.startsWith(Constants.HTTP) && !url.startsWith(Constants.HTTPS)) {
											url = new StringBuilder(feedUrl).append('/').append(url).toString();
										}
										values.put(FeedColumns.URL, url);
										cr.update(FeedColumns.CONTENT_URI(id), values, null, null);
										connection.disconnect();
										connection = setupConnection(url);
										contentType = connection.getContentType();
										break;
									}
								}
							}
						}
						// this indicates a badly configured feed
						if (posStart == -1) {
							connection.disconnect();
							connection = setupConnection(feedUrl);
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
							} catch (UnsupportedEncodingException usee) {
								fetchMode = FETCHMODE_REENCODE;
							}
						} else {
							fetchMode = FETCHMODE_REENCODE;
						}

					} else {
						BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(getConnectionInputStream(connection)));

						char[] chars = new char[20];

						int length = bufferedReader.read(chars);

						String xmlDescription = new String(chars, 0, length);

						connection.disconnect();
						connection = setupConnection(connection.getURL());

						int start = xmlDescription != null ? xmlDescription.indexOf(ENCODING) : -1;

						if (start > -1) {
							try {
								Xml.findEncodingByName(xmlDescription.substring(start + 10, xmlDescription.indexOf('"', start + 11)));
								fetchMode = FETCHMODE_DIRECT;
							} catch (UnsupportedEncodingException usee) {
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

						InputStream inputStream = getConnectionInputStream(connection);
						Xml.parse(inputStream,
								Xml.findEncodingByName(index2 > -1 ? contentType.substring(index + 8, index2) : contentType.substring(index + 8)),
								handler);
					} else {
						InputStreamReader reader = new InputStreamReader(getConnectionInputStream(connection));
						Xml.parse(reader, handler);
					}
					break;
				}
				case FETCHMODE_REENCODE: {
					ByteArrayOutputStream ouputStream = new ByteArrayOutputStream();
					InputStream inputStream = getConnectionInputStream(connection);

					byte[] byteBuffer = new byte[4096];

					int n;
					while ((n = inputStream.read(byteBuffer)) > 0) {
						ouputStream.write(byteBuffer, 0, n);
					}

					String xmlText = ouputStream.toString();

					int start = xmlText != null ? xmlText.indexOf(ENCODING) : -1;

					if (start > -1) {
						Xml.parse(
								new StringReader(new String(ouputStream.toByteArray(),
										xmlText.substring(start + 10, xmlText.indexOf('"', start + 11)))), handler);
					} else {
						// use content type
						if (contentType != null) {
							int index = contentType.indexOf(CHARSET);

							if (index > -1) {
								int index2 = contentType.indexOf(';', index);

								try {
									StringReader reader = new StringReader(new String(ouputStream.toByteArray(), index2 > -1 ? contentType.substring(
											index + 8, index2) : contentType.substring(index + 8)));
									Xml.parse(reader, handler);
								} catch (Exception e) {
								}
							} else {
								StringReader reader = new StringReader(new String(ouputStream.toByteArray()));
								Xml.parse(reader, handler);
							}
						}
					}
					break;
				}
				}

				connection.disconnect();
			} catch (FileNotFoundException e) {
				if (handler == null || (handler != null && !handler.isDone() && !handler.isCancelled())) {
					ContentValues values = new ContentValues();

					// resets the fetchmode to determine it again later
					values.put(FeedColumns.FETCH_MODE, 0);

					values.put(FeedColumns.ERROR, getString(R.string.error_feed_error));
					if (cr.update(FeedColumns.CONTENT_URI(id), values, null, null) > 0) {
						FeedDataContentProvider.notifyGroupFromFeedId(id);
					}
				}
			} catch (Throwable e) {
				if (handler == null || (handler != null && !handler.isDone() && !handler.isCancelled())) {
					ContentValues values = new ContentValues();

					// resets the fetchmode to determine it again later
					values.put(FeedColumns.FETCH_MODE, 0);

					values.put(FeedColumns.ERROR, e.getMessage() != null ? e.getMessage() : getString(R.string.error_feed_process));
					if (cr.update(FeedColumns.CONTENT_URI(id), values, null, null) > 0) {
						FeedDataContentProvider.notifyGroupFromFeedId(id);
					}
				}
			} finally {

				/* check and optionally find favicon */
				try {
					if (handler != null && cursor.getBlob(iconPosition) == null) {
						String feedLink = handler.getFeedLink();
						if (feedLink != null) {
							retrieveFavicon(this, new URL(feedLink), id);
						} else {
							retrieveFavicon(this, connection.getURL(), id);
						}
					}
				} catch (Throwable e) {
				}

				if (connection != null) {
					connection.disconnect();
				}
			}
		}

		cursor.close();

		return handler != null ? handler.getNewCount() : 0;
	}

	public static final HttpURLConnection setupConnection(String url) throws IOException, NoSuchAlgorithmException, KeyManagementException {
		return setupConnection(new URL(url));
	}

	public static final HttpURLConnection setupConnection(URL url) throws IOException, NoSuchAlgorithmException, KeyManagementException {
		return setupConnection(url, 0);
	}

	public static final HttpURLConnection setupConnection(URL url, int cycle) throws IOException, NoSuchAlgorithmException, KeyManagementException {
		HttpURLConnection connection = proxy == null ? (HttpURLConnection) url.openConnection() : (HttpURLConnection) url.openConnection(proxy);

		connection.setDoInput(true);
		connection.setDoOutput(false);
		connection.setRequestProperty(KEY_USERAGENT, VALUE_USERAGENT); // some feeds need this to work properly
		connection.setConnectTimeout(30000);
		connection.setReadTimeout(30000);
		connection.setUseCaches(false);

		if (url.getUserInfo() != null) {
			connection.setRequestProperty("Authorization", "Basic " + Base64.encode(url.getUserInfo().getBytes(), Base64.DEFAULT));
		}

		connection.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		connection.connect();

		String location = connection.getHeaderField("Location");

		if (location != null
				&& (url.getProtocol().equals(_HTTP) && location.startsWith(Constants.HTTPS) || url.getProtocol().equals(_HTTPS)
						&& location.startsWith(Constants.HTTP))) {
			// if location != null, the system-automatic redirect has failed
			// which indicates a protocol change

			connection.disconnect();

			if (cycle < 5) {
				return setupConnection(new URL(location), cycle + 1);
			} else {
				throw new IOException("Too many redirects.");
			}
		}
		return connection;
	}

	public static byte[] getBytes(InputStream inputStream) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		byte[] buffer = new byte[4096];

		int n;

		while ((n = inputStream.read(buffer)) > 0) {
			output.write(buffer, 0, n);
		}

		byte[] result = output.toByteArray();

		output.close();
		inputStream.close();
		return result;
	}

	private static void retrieveFavicon(Context context, URL url, String id) {
		HttpURLConnection iconURLConnection;
		try {
			iconURLConnection = setupConnection(new URL(new StringBuilder(url.getProtocol()).append(PROTOCOL_SEPARATOR).append(url.getHost())
					.append(FILE_FAVICON).toString()));

			ContentValues values = new ContentValues();
			try {
				byte[] iconBytes = getBytes(getConnectionInputStream(iconURLConnection));
				values.put(FeedColumns.ICON, iconBytes);
			} catch (Exception e) {
				// no icon found or error
				values.put(FeedColumns.ICON, new byte[0]);
			} finally {
				iconURLConnection.disconnect();

				context.getContentResolver().update(FeedColumns.CONTENT_URI(id), values, null, null);
				FeedDataContentProvider.notifyGroupFromFeedId(id);
			}
		} catch (Throwable t) {
		}
	}

	/**
	 * This is a small wrapper for getting the properly encoded inputstream if is is gzip compressed and not properly recognized.
	 */
	public static InputStream getConnectionInputStream(HttpURLConnection connection) throws IOException {
		InputStream inputStream = connection.getInputStream();

		if (GZIP.equals(connection.getContentEncoding()) && !(inputStream instanceof GZIPInputStream)) {
			return new GZIPInputStream(inputStream);
		} else {
			return inputStream;
		}
	}

	public static boolean isCurrentlyFetching() {
		ActivityManager manager = (ActivityManager) MainApplication.getAppContext().getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (FetcherService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
}
