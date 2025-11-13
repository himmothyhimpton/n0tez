package com.n0tez.app

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

class N0tezApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        try {
            FirebaseApp.initializeApp(this)
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            FirebaseCrashlytics.getInstance().setCustomKey("app_version", pInfo.versionName ?: "")
            val buildCode = try { pInfo.longVersionCode } catch (e: Throwable) { pInfo.versionCode.toLong() }
            FirebaseCrashlytics.getInstance().setCustomKey("app_build", buildCode)
        } catch (e: Throwable) {
            android.util.Log.e("N0tezApplication", "Firebase init error", e)
        }
    }
    
    companion object {
        private var instance: N0tezApplication? = null
        
        fun getContext(): Context {
            return instance?.applicationContext ?: throw IllegalStateException("Application not initialized")
        }
    }
}
