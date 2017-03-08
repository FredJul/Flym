package net.frju.flym.data.db

import android.content.Context
import net.frju.androidquery.database.BaseLocalDatabaseProvider
import net.frju.androidquery.database.Resolver
import net.frju.androidquery.gen.FEED
import net.frju.androidquery.gen.Q
import net.frju.flym.data.Feed
import org.jetbrains.anko.doAsync
import java.util.*


class LocalDatabaseProvider(context: Context) : BaseLocalDatabaseProvider(context) {

    override fun getDbName(): String {
        return "local_models"
    }

    override fun getDbVersion(): Int {
        return 1
    }

    override fun getResolver(): Resolver {
        return Q.getResolver()
    }

    override fun onPostCreate() {
        doAsync {
            addDefaultRestrictions()
        }
    }

    private fun addDefaultRestrictions() {
        val feed = Feed()
        feed.id = UUID.randomUUID().toString()
        feed.title = "Default rss"
        feed.link = "https://news.google.fr/?output=rss"

        FEED.insert(feed).query()
    }
}
