package net.frju.flym.ui.entrydetails

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem

class EntryDetailsActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		if (savedInstanceState == null) {
			val fragment = EntryDetailsFragment().apply {
				arguments = intent.extras
			}

			supportFragmentManager
					.beginTransaction()
					.replace(android.R.id.content, fragment)
					.commitAllowingStateLoss()
		}
	}

	override fun onOptionsItemSelected(item: MenuItem?): Boolean {
		when (item?.itemId) {
			android.R.id.home -> onBackPressed()
		}

		return false
	}
}
