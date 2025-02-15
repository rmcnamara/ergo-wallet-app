package org.ergoplatform.android

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import org.ergoplatform.NodeConnector
import org.ergoplatform.appkit.NetworkType
import org.ergoplatform.isErgoMainNet
import org.ergoplatform.utils.LogUtils

class App : Application() {

    companion object {
        var lastStackTrace: String? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        isErgoMainNet = (StageConstants.NETWORK_TYPE == NetworkType.MAINNET)
        val preferences = Preferences(applicationContext)
        AppCompatDelegate.setDefaultNightMode(preferences.dayNightMode)
        NodeConnector.getInstance().loadPreferenceValues(preferences)

        LogUtils.stackTraceLogger = { lastStackTrace = it }
    }
}