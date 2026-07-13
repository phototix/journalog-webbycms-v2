package com.journalog.app

import android.app.Application
import android.util.Log

class JournalogApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("Journalog-Crash", "Uncaught exception on thread: ${thread.name}", throwable)
        }
    }

    companion object {
        lateinit var instance: JournalogApp
            private set
    }
}
