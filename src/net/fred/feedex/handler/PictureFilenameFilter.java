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

package net.fred.feedex.handler;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

import net.fred.feedex.provider.FeedDataContentProvider;

public class PictureFilenameFilter implements FilenameFilter {
	private static final String REGEX = "__[^\\.]*\\.[A-Za-z]*";

	private Pattern pattern;

	public PictureFilenameFilter(String entryId) {
		setEntryId(entryId);
	}

	public PictureFilenameFilter() {
	}

	public void setEntryId(String entryId) {
		pattern = Pattern.compile(entryId + REGEX);
	}

	@Override
	public boolean accept(File dir, String filename) {
		if (dir != null && dir.equals(FeedDataContentProvider.IMAGE_FOLDER_FILE)) { // this should be always true but lets check it anyway
			return pattern.matcher(filename).find();
		} else {
			return false;
		}
	}
}
