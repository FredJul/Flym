package net.frju.flym.ui.main;

import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;

import net.frju.flym.data.Feed;

import java.util.List;


public class FeedGroup extends ExpandableGroup<Feed> {

    public FeedGroup(String title, List<Feed> items) {
        super(title, items);
    }
}