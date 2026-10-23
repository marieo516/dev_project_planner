package com.example.dev_project_planner

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment

class AboutFragment : Fragment(R.layout.fragment_about) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val language = requireContext()
            .getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
            .getString("language", "en")

        val descriptionFile = if (language == "fr") {
            "project_description_fr.txt"
        } else {
            "project_description_en.txt"
        }

        val description = requireContext().assets.open(descriptionFile)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText().trim() }

        view.findViewById<TextView>(R.id.about_description).text = description
    }
}
