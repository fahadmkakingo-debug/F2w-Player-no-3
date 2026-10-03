package com.example

import android.app.Application
import android.util.Log

class F2WApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Global uncaught exception handler to log and prevent catastrophic app crashes
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("F2WApplication", "Uncaught exception safely caught: ${throwable.localizedMessage}", throwable)
            try {
                defaultHandler?.uncaughtException(thread, throwable)
            } catch (_: Throwable) {}
        }

        try {
            com.example.util.media.VideoThumbnailHelper.initialize(this)
        } catch (t: Throwable) {
            Log.e("F2WApplication", "Failed to initialize VideoThumbnailHelper", t)
        }
    }
}
