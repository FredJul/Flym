package net.frju.flym.ui.items

import android.content.Context
import net.frju.androidquery.gen.FEED
import net.frju.androidquery.gen.ITEM
import net.frju.androidquery.operation.function.CursorResult
import net.frju.androidquery.operation.join.Join.innerJoin
import net.frju.androidquery.support.v4.database.BaseSelectLoader
import net.frju.flym.data.Feed
import net.frju.flym.data.Item


class ItemsLoader(context: Context) : BaseSelectLoader<Item>(context) {

    override fun doSelect(): CursorResult<Item> {
        return ITEM.select()
                .join(innerJoin(Item::class.java, ITEM.FEED_ID, Feed::class.java, FEED.ID))
                .query()
    }
}