package net.frju.flym

import android.app.Application
import android.content.Context
import net.frju.androidquery.gen.Q

/**
 * Created by Lucas on 02/01/2017.
 */

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        context = applicationContext

        Q.init(this)
    }

    companion object {
        @JvmStatic
        var context: Context? = null
            private set
    }
}
