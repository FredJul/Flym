package net.fred.feedex.adapter;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import net.fred.feedex.R;
import net.fred.feedex.activity.MainActivity;

public class MenuListAdapter extends BaseAdapter {
    private Context context;
    private SparseArray<MainActivity.feedObject> mFeed = new SparseArray<MainActivity.feedObject>();

    public MenuListAdapter(Context pContext, SparseArray<MainActivity.feedObject> feed){
        context = pContext;
        mFeed = feed;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View itemView=null;
        MainActivity.feedObject feed = mFeed.get(position);
        if (feed.isGroup==true){//Groups
            itemView = inflater.inflate(R.layout.drawer_list_header, parent, false);
            TextView txtTitle = (TextView) itemView.findViewById(R.id.list_header_title);
            txtTitle.setText(feed.feedOrgroupName);
            if(feed.feedCount!=-1){
                TextView countFeed = (TextView) itemView.findViewById(R.id.menurow_counter_header);
                countFeed.setText(""+feed.feedCount);
            }
        }  else{//Feeds
            itemView = inflater.inflate(R.layout.drawer_list_item, parent, false);
            TextView txtTitle = (TextView) itemView.findViewById(R.id.title);
            /*icon*/
            if (feed.iconFeed != null && feed.iconFeed.getHeight() > 0 && feed.iconFeed.getWidth() > 0) {
                ImageView imgIcon = (ImageView) itemView.findViewById(R.id.icon);
                imgIcon.setImageBitmap(feed.iconFeed);
            }
            /*Count*/
            if(feed.feedCount!=-1){
                TextView countFeed = (TextView) itemView.findViewById(R.id.menurow_counter);
                countFeed.setText(""+feed.feedCount);
            }
            txtTitle.setText(feed.feedOrgroupName);
        }
        return itemView;
    }

    @Override
    public int getCount() {
        return mFeed.size();
    }

    @Override
    public Object getItem(int position) {
        return mFeed.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}