package com.example.dev_project_planner

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

fun applySavedTheme(ctx: Context) {
    val saved = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("theme", "system")
    AppCompatDelegate.setDefaultNightMode(
        when (saved) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    )
}
