package com.example.dev_project_planner

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment

const val EXTRA_PROJECT_ID = "project_id"

fun Context.dp(v: Int) = (v * resources.displayMetrics.density).toInt()

fun Fragment.currentProject(): Project? =
    Db.find(requireActivity().intent.getStringExtra(EXTRA_PROJECT_ID))

fun AppCompatActivity.applyInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
        insets
    }
}
