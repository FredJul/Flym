package net.frju.flym.ui.main

import net.frju.flym.data.entities.EntryWithFeed
import net.frju.flym.data.entities.Feed

interface MainNavigator {

    enum class State {
        SINGLE_COLUMN_MASTER, SINGLE_COLUMN_DETAILS, TWO_COLUMNS_EMPTY, TWO_COLUMNS_WITH_DETAILS
    }

    fun goToEntriesList(feed: Feed?)

    fun goToEntryDetails(entry: EntryWithFeed)

    fun goToSettings()

    fun goToFeedback()

    fun goToPreviousEntry()

    fun goToNextEntry()

}
