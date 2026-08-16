package io.github.fate_grand_automata.util

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import io.github.fate_grand_automata.BuildConfig
import io.github.fate_grand_automata.prefs.core.PrefsCore
import org.opencv.android.OpenCVLoader
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class AutomataApplication : Application() {
    @Inject
    lateinit var prefsCore: PrefsCore

    override fun onCreate() {
        super.onCreate()

        // Test mode is deliberately one-shot across application process starts.
        prefsCore.dreamFireTestMode.set(false)

        initLogging()

        OpenCVLoader.initLocal()
    }

    private fun initLogging() {
        Timber.plant(FgaTree())
    }

    private class FgaTree : Timber.DebugTree() {
        override fun isLoggable(tag: String?, priority: Int): Boolean {
            return if (BuildConfig.DEBUG) true else priority > Log.INFO
        }
    }
}
