package com.n0tez.app

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

class N0tezApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Enable Crashlytics
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        
        // Set user properties for analytics
        FirebaseCrashlytics.getInstance().setCustomKey("app_version", BuildConfig.VERSION_NAME)
        FirebaseCrashlytics.getInstance().setCustomKey("app_build", BuildConfig.VERSION_CODE)
    }
    
    companion object {
        private var instance: N0tezApplication? = null
        
        fun getContext(): Context {
            return instance?.applicationContext ?: throw IllegalStateException("Application not initialized")
        }
    }
}