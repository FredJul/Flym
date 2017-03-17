package net.frju.flym.ui.main

import net.frju.flym.data.Feed
import net.frju.flym.data.Item

/**
 * Created by Lucas on 02/01/2017.
 */

interface MainNavigator {

    enum class State {
        SINGLE_COLUMN_MASTER, SINGLE_COLUMN_DETAILS, TWO_COLUMNS_EMPTY, TWO_COLUMNS_WITH_DETAILS
    }

    fun goToItemsList(feed: Feed?)

    fun goToItemDetails(item: Item)

    fun goToSettings()

    fun goToFeedback()

    fun goToPreviousItem()

    fun goToNextItem()

}
