package com.example.dev_project_planner

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText

class ProjectsFragment : Fragment(R.layout.fragment_projects) {

    private var query = ""
    private var labelFilter: String? = null
    private var sortBy = "Due date"
    private lateinit var projectAdapter: ProjectAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        projectAdapter = ProjectAdapter { project ->
            startActivity(
                Intent(requireContext(), ProjectActivity::class.java)
                    .putExtra(EXTRA_PROJECT_ID, project.id)
            )
        }
        view.findViewById<RecyclerView>(R.id.project_list).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = projectAdapter
        }

        view.findViewById<TextInputEditText>(R.id.search).doAfterTextChanged {
            query = it?.toString().orEmpty().trim()
            refresh()
        }

        val sortButton = view.findViewById<MaterialButton>(R.id.sort)
        sortButton.setOnClickListener { anchor ->
            val popup = PopupMenu(requireContext(), anchor)
            listOf("Due date", "Name", "Status").forEach { popup.menu.add(ui(requireContext(), it)) }
            popup.setOnMenuItemClickListener { item ->
                sortBy = listOf("Due date", "Name", "Status").first {
                    ui(requireContext(), it) == item.title.toString()
                }
                sortButton.text = "${ui(requireContext(), "Sort")}: ${ui(requireContext(), sortBy)}"
                refresh()
                true
            }
            popup.show()
        }

        view.findViewById<View>(R.id.fab).setOnClickListener {
            startActivity(Intent(requireContext(), EditProjectActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        buildLabelTags()
        refresh()
    }

    private fun buildLabelTags() {
        val group = requireView().findViewById<ChipGroup>(R.id.label_filters)
        group.removeAllViews()
        val current = labelFilter
        if (current != null && Db.label(current) == null) labelFilter = null

        val all = makeTag(requireContext(), group, ui(requireContext(), "All"), selectable = true)
        all.isChecked = labelFilter == null
        all.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                labelFilter = null
                refresh()
            }
        }
        group.addView(all)

        for (label in Db.labels) {
            val tag = makeTag(requireContext(), group, label.name, selectable = true)
            tag.isChecked = labelFilter == label.id
            tag.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    labelFilter = label.id
                    refresh()
                }
            }
            group.addView(tag)
        }
    }

    private fun refresh() {
        val v = view ?: return
        val list = Db.search(query, labelFilter, sortBy)
        projectAdapter.submit(list)
        v.findViewById<TextView>(R.id.count).text =
            if (list.size == 1) {
                ui(requireContext(), "1 project")
            } else {
                "${list.size}${ui(requireContext(), " projects")}"
            }
    }
}
