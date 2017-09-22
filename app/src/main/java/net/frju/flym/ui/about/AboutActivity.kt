package net.frju.flym.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.vansuita.materialabout.builder.AboutBuilder
import net.fred.feedex.R


class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val view = AboutBuilder.with(this)
                .setPhoto(R.mipmap.profile_picture)
                .setCover(R.mipmap.profile_cover)
                .setName("Frédéric Julian")
                .setBrief("I'm publishing this application as free and open-source software under GPLv3 licence. Feel free to modify it as long as you keep it open-source as well.")
                .setAppIcon(R.mipmap.ic_launcher_foreground)
                .setAppName(R.string.app_name)
                .addGitHubLink("FredJul")
                .addFiveStarsAction()
                .addShareAction(R.string.app_name)
                .setWrapScrollView(true)
                .setLinksAnimated(true)
                .setShowAsCard(true)
                .addDonateAction {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.me/fredericjulian")))
                }
                .build()

        setContentView(view)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> onBackPressed()
        }

        return true
    }
}