package br.com.apolo.booster

import android.app.Application
import br.com.apolo.booster.audio.BoostController

class ApoloApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BoostController.init(this)
    }
}
