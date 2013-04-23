/**
 * FeedEx
 * 
 * Copyright (c) 2012-2013 Frederic Julian
 * Copyright (c) 2010-2012 Stefan Handschuh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.fred.feedex;

import java.text.DateFormat;

public final class Constants {
	public static final DateFormat DATE_FORMAT = android.text.format.DateFormat.getDateFormat(MainApplication.getAppContext());
	public static final DateFormat TIME_FORMAT = android.text.format.DateFormat.getTimeFormat(MainApplication.getAppContext());

	public static final String ACTION_REFRESH_FEEDS = "net.fred.feedex.REFRESH";
	public static final String ACTION_REFRESH_FINISHED = "net.fred.feedex.REFRESH_FINISHED";
	public static final String ACTION_MOBILIZE_FEED = "net.fred.feedex.MOBILIZE_FEED";

	public static final String FEED_ID = "feedid";

	public static final String ENTRY_URI = "entry_uri";

	public static final String DB_IS_NULL = " IS NULL";
	public static final String DB_DESC = " DESC";
	public static final String DB_ARG = "=?";
	public static final String DB_AND = " AND ";
	public static final String DB_OR = " OR ";

	public static final String HTTP = "http://";
	public static final String HTTPS = "https://";
	public static final String _HTTP = "http";
	public static final String _HTTPS = "https";
	public static final String PROTOCOL_SEPARATOR = "://";

	public static final String FILE_FAVICON = "/favicon.ico";

	public static final String HTML_TAG_REGEX = "<(.|\n)*?>";

	public static final String FILE_URL = "file://";

	public static final String IMAGEFILE_IDSEPARATOR = "__";

	public static final String IMAGEID_REPLACEMENT = "##ID##";

	public static final String DEFAULT_PROXY_PORT = "8080";

	public static final String HTML_SPAN_REGEX = "<[/]?[ ]?span(.|\n)*?>";
	public static final String HTML_IMG_REGEX = "<[/]?[ ]?img(.|\n)*?>";

	public static final String ONE = "1";

	public static final String HTML_LT = "&lt;";
	public static final String HTML_GT = "&gt;";
	public static final String LT = "<";
	public static final String GT = ">";

	public static final String TRUE = "true";
	public static final String FALSE = "false";

	public static final String ENCLOSURE_SEPARATOR = "[@]"; // exactly three characters!

	public static final String HTML_QUOT = "&quot;";
	public static final String QUOT = "\"";
	public static final String HTML_APOSTROPHE = "&#39;";
	public static final String APOSTROPHE = "'";
	public static final String AMP = "&";
	public static final String AMP_SG = "&amp;";
	public static final String SLASH = "/";
	public static final String COMMA_SPACE = ", ";

	public static final String SCHEDULED = "scheduled";
}
