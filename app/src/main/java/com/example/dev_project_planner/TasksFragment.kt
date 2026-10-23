package com.example.dev_project_planner

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class TasksFragment : Fragment(R.layout.fragment_tasks) {

    private val collapsed = mutableSetOf<String>()
    private var showCompleted = true
    private lateinit var taskAdapter: TaskAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val p = currentProject() ?: return

        view.findViewById<TextView>(R.id.project_name).text = p.name
        view.findViewById<View>(R.id.btn_back).setOnClickListener { requireActivity().finish() }

        taskAdapter = TaskAdapter(
            project = p,
            onToggle = { task, checked ->
                Db.setTaskDone(p, task, checked)
                refresh()
            },
            onArrow = { task ->
                if (!collapsed.remove(task.id)) collapsed.add(task.id)
                refresh()
            },
            onAddChild = { task ->
                requireContext().askText(
                    "${ui(requireContext(), "Subtask of")} \"${task.title}\"",
                    ui(requireContext(), "Task title")
                ) {
                    Db.insertTask(p, it, task.id)
                    collapsed.remove(task.id)
                    refresh()
                }
            },
            onDelete = { task ->
                val message = "${ui(requireContext(), "Delete")} \"${task.title}\" " +
                    ui(requireContext(), "and everything under it?")
                confirm(requireContext(), message, ui(requireContext(), "Delete")) {
                    Db.deleteTask(p, task)
                    refresh()
                }
            }
        )
        view.findViewById<RecyclerView>(R.id.task_list).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
        }

        view.findViewById<CompoundButton>(R.id.check_show_completed)
            .setOnCheckedChangeListener { _, checked ->
                showCompleted = checked
                refresh()
            }

        val collapseButton = view.findViewById<MaterialButton>(R.id.btn_collapse_all)
        collapseButton.setOnClickListener {
            val groups = p.tasks.filter { p.hasChildren(it) }.map { it.id }
            if (groups.isNotEmpty() && collapsed.containsAll(groups)) collapsed.clear()
            else collapsed.addAll(groups)
            collapseButton.text = ui(requireContext(), if (collapsed.isNotEmpty()) "Expand all" else "Collapse all")
            refresh()
        }

        view.findViewById<View>(R.id.btn_add_task).setOnClickListener {
            requireContext().askText(
                ui(requireContext(), "New task"),
                ui(requireContext(), "Task title")
            ) {
                Db.insertTask(p, it, null)
                refresh()
            }
        }

        refresh()
    }

    private fun refresh() {
        val p = currentProject() ?: return
        val rows = mutableListOf<TaskRow>()

        fun walk(parent: String?, depth: Int) {
            for (t in p.children(parent)) {
                if (!showCompleted && p.isDone(t)) continue
                val isCollapsed = t.id in collapsed
                rows.add(TaskRow(t, depth, isCollapsed))
                if (!isCollapsed) walk(t.id, depth + 1)
            }
        }
        walk(null, 0)
        taskAdapter.submit(rows)
    }
}
