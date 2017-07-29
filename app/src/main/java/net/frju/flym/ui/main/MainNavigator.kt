package net.frju.flym.ui.main

import net.frju.flym.data.entities.Feed
import net.frju.flym.data.entities.Item

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
