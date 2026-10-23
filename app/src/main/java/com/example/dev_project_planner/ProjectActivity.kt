package com.example.dev_project_planner

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class ProjectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_project)
        applyInsets()

        if (savedInstanceState == null) show(OverviewFragment())

        findViewById<BottomNavigationView>(R.id.bottom_nav)
            .setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_overview -> show(OverviewFragment())
                    R.id.nav_planning -> show(PlanningFragment())
                    R.id.nav_tasks -> show(TasksFragment())
                }
                true
            }
    }

    override fun onResume() {
        super.onResume()
        if (Db.find(intent.getStringExtra(EXTRA_PROJECT_ID)) == null) finish()
    }

    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
