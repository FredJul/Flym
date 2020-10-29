package net.frju.flym.data.entities

import org.junit.Assert.assertEquals
import org.junit.Test

class FeedTest {

    @Test
    fun getLettersForName_empty_feed_title() {
        val letters = Feed.getLettersForName("")
        assertEquals("", letters)
    }

    @Test
    fun getLettersForName_single_letter_feed_title() {
        val letters = Feed.getLettersForName("a")
        assertEquals("A", letters)
    }
}
