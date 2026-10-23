package com.example.dev_project_planner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.chip.ChipGroup
import java.io.File

class OverviewFragment : Fragment(R.layout.fragment_overview) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.btn_back).setOnClickListener { requireActivity().finish() }
        view.findViewById<View>(R.id.btn_edit).setOnClickListener {
            val p = currentProject() ?: return@setOnClickListener
            startActivity(
                Intent(requireContext(), EditProjectActivity::class.java)
                    .putExtra(EXTRA_PROJECT_ID, p.id)
            )
        }
        view.findViewById<View>(R.id.btn_export).setOnClickListener {
            val project = currentProject() ?: return@setOnClickListener
            try {
                val location = exportProjectSummary(requireContext(), project)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.export_complete, location),
                    Toast.LENGTH_LONG
                ).show()
            } catch (_: Exception) {
                Toast.makeText(requireContext(), R.string.export_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val p = currentProject() ?: return
        val v = requireView()

        v.findViewById<TextView>(R.id.project_name).text = p.name
        v.findViewById<TextView>(R.id.project_status).text = ui(requireContext(), p.status)
        v.findViewById<TextView>(R.id.project_description).apply {
            text = p.description
            visibility = if (p.description.isBlank()) View.GONE else View.VISIBLE
        }

        val labels = v.findViewById<ChipGroup>(R.id.project_labels)
        labels.removeAllViews()
        p.labelIds.mapNotNull { Db.label(it) }.forEach {
            labels.addView(makeTag(requireContext(), labels, it.name, dotColor = it.color, clickable = false))
        }

        val dates = v.findViewById<LinearLayout>(R.id.dates_list)
        dates.removeAllViews()
        p.start?.let { addRow(dates, ui(requireContext(), "Start date"), formatDate(it)) }
        p.due?.let { addRow(dates, ui(requireContext(), "Due date"), formatDate(it)) }

        val sprints = v.findViewById<LinearLayout>(R.id.sprints_list)
        sprints.removeAllViews()
        val now = System.currentTimeMillis()
        p.sprints.forEach { s ->
            val current = now >= s.start && now < s.end + DAY_MS
            addRow(sprints, s.name, "${formatDate(s.start)} – ${formatDate(s.end)}", current) {
                val message = "${ui(requireContext(), "Remove")} \"${s.name}\"?"
                confirm(requireContext(), message, ui(requireContext(), "Remove")) {
                    p.sprints.remove(s)
                    Db.save(p)
                    render()
                }
            }
        }

        val imageRow = v.findViewById<LinearLayout>(R.id.image_row)
        imageRow.removeAllViews()
        p.images.forEach { path ->
            val item = layoutInflater.inflate(R.layout.item_image, imageRow, false)
            item.findViewById<ImageView>(R.id.image).setImageURI(Uri.fromFile(File(path)))
            item.findViewById<View>(R.id.btn_remove).visibility = View.GONE
            imageRow.addView(item)
        }
    }

    private fun addRow(
        parent: LinearLayout, label: String, value: String,
        current: Boolean = false, onClick: (() -> Unit)? = null
    ) {
        val row = layoutInflater.inflate(R.layout.item_row_pair, parent, false)
        row.findViewById<TextView>(R.id.row_label).text = label
        row.findViewById<View>(R.id.row_badge).visibility = if (current) View.VISIBLE else View.GONE
        row.findViewById<TextView>(R.id.row_value).text = value
        if (onClick != null) row.setOnClickListener { onClick() } else row.isClickable = false
        parent.addView(row)
    }

}
