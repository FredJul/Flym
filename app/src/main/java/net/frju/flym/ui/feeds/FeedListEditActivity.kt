package net.frju.flym.ui.feeds

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import net.fred.feedex.R

class FeedListEditActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		setContentView(R.layout.activity_feed_list_edit)
	}

	override fun onOptionsItemSelected(item: MenuItem?): Boolean {
		when (item?.itemId) {
			android.R.id.home -> onBackPressed()
		}

		return true
	}
}
