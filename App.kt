package com.kimipro.launcher

import android.app.Application

class App : Application() {
    companion object {
        lateinit var app: Application
            private set
    }

    override fun onCreate() {
        super.onCreate()
        app = this
    }
}
