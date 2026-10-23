package com.example.dev_project_planner

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySavedTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        applyInsets()

        if (savedInstanceState == null) show(ProjectsFragment())

        findViewById<BottomNavigationView>(R.id.bottom_nav)
            .setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_projects -> show(ProjectsFragment())
                    R.id.nav_about -> show(AboutFragment())
                    R.id.nav_settings -> show(SettingsFragment())
                }
                true
            }
    }

    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
