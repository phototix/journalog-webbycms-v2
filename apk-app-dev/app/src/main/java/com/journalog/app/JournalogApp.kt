package com.journalog.app

import android.app.Application

class JournalogApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: JournalogApp
            private set
    }
}
