package net.frju.flym.ui.settings

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import net.fred.feedex.R

class SettingsActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.activity_settings)

		supportActionBar?.setDisplayHomeAsUpEnabled(true)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			android.R.id.home -> {
				finish()
			}
		}
		return true
	}
}