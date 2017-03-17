package net.frju.flym.ui.items

import android.content.Context
import net.frju.androidquery.gen.FEED
import net.frju.androidquery.gen.ITEM
import net.frju.androidquery.operation.condition.Where
import net.frju.androidquery.operation.function.CursorResult
import net.frju.androidquery.operation.join.Join.innerJoin
import net.frju.androidquery.support.v4.database.BaseSelectLoader
import net.frju.flym.data.Feed
import net.frju.flym.data.Item


class ItemsLoader(context: Context, val feed: Feed?) : BaseSelectLoader<Item>(context) {

    override fun doSelect(): CursorResult<Item> {
        if (feed == null || feed.id == Feed.UNREAD_ITEMS_ID) {
            return ITEM.select()
                    .join(innerJoin(Item::class.java, ITEM.FEED_ID, Feed::class.java, FEED.ID))
                    .where(Where.field(Item::class.java.simpleName + '.' + ITEM.READ).isFalse)
                    .query()
        } else if (feed.id == Feed.ALL_ITEMS_ID) {
            return ITEM.select()
                    .join(innerJoin(Item::class.java, ITEM.FEED_ID, Feed::class.java, FEED.ID))
                    .query()
        } else if (feed.id == Feed.FAVORITES_ID) {
            return ITEM.select()
                    .join(innerJoin(Item::class.java, ITEM.FEED_ID, Feed::class.java, FEED.ID))
                    .where(Where.field(Item::class.java.simpleName + '.' + ITEM.FAVORITE).isTrue)
                    .query()
        } else if (feed.isGroup) {
            return ITEM.select()
                    .join(innerJoin(Item::class.java, ITEM.FEED_ID, Feed::class.java, FEED.ID))
                    .where(Where.field(Feed::class.java.simpleName + '.' + FEED.GROUP_ID).isEqualTo(feed.id))
                    .query()
        } else {
            return ITEM.select()
                    .join(innerJoin(Item::class.java, ITEM.FEED_ID, Feed::class.java, FEED.ID))
                    .where(Where.field(Item::class.java.simpleName + '.' + ITEM.FEED_ID).isEqualTo(feed.id))
                    .query()
        }
    }
}