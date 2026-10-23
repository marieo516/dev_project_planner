package com.example.dev_project_planner

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Db.init(this)
    }
}
