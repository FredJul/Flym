package ru.yanus171.feedexfork.parser;

import android.content.ContentResolver;
import android.database.Cursor;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.yanus171.feedexfork.MainApplication;
import ru.yanus171.feedexfork.provider.FeedData;
import ru.yanus171.feedexfork.utils.PrefUtils;

class FeedFilters {

    private final ArrayList<Rule> mFilters = new ArrayList<>();

    public FeedFilters(String feedId) {
        ContentResolver cr = MainApplication.getContext().getContentResolver();
        Cursor c = cr.query(FeedData.FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(feedId), new String[]{FeedData.FilterColumns.FILTER_TEXT, FeedData.FilterColumns.IS_REGEX,
                FeedData.FilterColumns.IS_APPLIED_TO_TITLE, FeedData.FilterColumns.IS_ACCEPT_RULE, FeedData.FilterColumns.IS_MARK_STARRED}, null, null, null);
        while (c.moveToNext()) {
            Rule r = new Rule();
            r.filterText = c.getString(0);
            r.isRegex = c.getInt(1) == 1;
            r.isAppliedToTitle = c.getInt(2) == 1;
            r.isAcceptRule = c.getInt(3) == 1;
            r.isMarkAsStarred = c.getInt(4) == 1;
            mFilters.add(r);
        }
        c.close();

        if ( PrefUtils.getBoolean( "global_marks_as_star_filter_on", false ) ) {
            String[] list = TextUtils.split( PrefUtils.getString( "global_marks_as_star_filter_rule",
                                                            "-------||||||-______" ),
                                    "\n"  );
            for ( String rule: list ) {
                if ( rule.trim().isEmpty() )
                    continue;
                Rule r = new Rule();
                r.filterText = rule;
                r.isRegex = PrefUtils.getBoolean("global_marks_as_star_filter_rule_is_regex", false);
                r.isAppliedToTitle = PrefUtils.getBoolean("global_marks_as_star_filter_apply_to_title", true);
                r.isAcceptRule = false;
                r.isMarkAsStarred = true;
                mFilters.add(r);
            }
        }

    }

    public boolean isEntryFiltered(String title, String author, String url, String content) {

        boolean isFiltered = false;

        for (Rule r : mFilters) {
            if ( r.isMarkAsStarred)
                continue;

            boolean isMatch = r.isMatch( title, author, url, content );

            if (r.isAcceptRule) {
                if (isMatch) {

                    isFiltered = false;
                    break; // accept rules override reject rules, the rest of the rules must be ignored
                } else {
                    isFiltered = true;
                    //break;
                }
            } else if (isMatch) {
                isFiltered = true;
                //break; // no break, there might be an accept rule later
            }
        }

        return isFiltered;
    }

    public boolean isMarkAsStarred(String title, String author, String url, String content) {
        for (Rule r : mFilters)
            if ( r.isMarkAsStarred && r.isMatch( title, author, url, content ) )
                return true;
        return false;
    }

    private class Rule {
        public String filterText;
        public boolean isRegex;
        public boolean isAppliedToTitle;
        public boolean isAcceptRule;
        public boolean isMarkAsStarred = false;

        boolean isMatch(String title, String author, String url, String content) {
            boolean result = false;
            author = author == null ? "" : author;
            if (isRegex) {
                Pattern p = Pattern.compile(filterText);
                if (isAppliedToTitle) {
                    Matcher mT = p.matcher(title);
                    Matcher mA = p.matcher(author);
                    Matcher mU = p.matcher(url);
                    result = mT.find() || mA.find() || mU.find();
                } else if (content != null) {
                    Matcher m = p.matcher(content);
                    result = m.find();
                }
            } else if ((isAppliedToTitle && (title != null && title.toLowerCase().contains(filterText.toLowerCase()) ||
                                             author != null && author.toLowerCase().contains(filterText.toLowerCase()) ||
                                             url.contains(filterText))) ||
                    (!isAppliedToTitle && content != null && content.toLowerCase().contains(filterText.toLowerCase()))) {
                result = true;
            }
            return result;
        }
    }
}
