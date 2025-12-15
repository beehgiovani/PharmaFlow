package com.developersbeeh.pharmaflow

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PharmaFlowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Aqui inicializaremos Firebase, Logs, etc. no futuro
    }
}