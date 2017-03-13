package net.frju.flym.data.db

import android.content.Context
import net.frju.androidquery.database.BaseLocalDatabaseProvider
import net.frju.androidquery.database.Resolver
import net.frju.androidquery.gen.FEED
import net.frju.androidquery.gen.Q
import net.frju.flym.data.Feed
import org.jetbrains.anko.doAsync


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
            addDefaultFeeds()
        }
    }

    private fun addDefaultFeeds() {
        val group1 = Feed()
        group1.title = "Default group"
        group1.isGroup = true

        FEED.insert(group1).query()

        val feedInsideGroup = Feed()
        feedInsideGroup.title = "Google News"
        feedInsideGroup.link = "https://news.google.fr/?output=rss"
        feedInsideGroup.groupId = group1.id

        val feedInsideGroup2 = Feed()
        feedInsideGroup2.title = "LeMonde"
        feedInsideGroup2.link = "http://www.lemonde.fr/rss/une.xml"
        feedInsideGroup2.groupId = group1.id

        val feedOutsideGroup = Feed()
        feedOutsideGroup.title = "The Register"
        feedOutsideGroup.link = "https://www.theregister.co.uk/software/headlines.atom"

        FEED.insert(feedInsideGroup, feedInsideGroup2, feedOutsideGroup).query()
    }
}
