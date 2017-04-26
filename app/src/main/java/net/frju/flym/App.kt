package net.frju.flym

import android.app.Application
import android.content.Context
import net.frju.androidquery.gen.Q

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        context = applicationContext

        Q.init(this)
    }

    companion object {
        @JvmStatic
        lateinit var context: Context
            private set
    }
}
