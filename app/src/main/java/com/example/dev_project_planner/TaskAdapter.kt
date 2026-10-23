package com.example.dev_project_planner

import android.graphics.Paint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class TaskRow(val task: Task, val depth: Int, val collapsed: Boolean)

class TaskAdapter(
    private val project: Project,
    private val onToggle: (Task, Boolean) -> Unit,
    private val onArrow: (Task) -> Unit,
    private val onAddChild: (Task) -> Unit,
    private val onDelete: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.VH>() {

    private var rows: List<TaskRow> = emptyList()

    fun submit(list: List<TaskRow>) {
        rows = list
        notifyDataSetChanged()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val arrow: TextView = view.findViewById(R.id.arrow)
        val check: CheckBox = view.findViewById(R.id.check)
        val title: TextView = view.findViewById(R.id.title)
        val progress: TextView = view.findViewById(R.id.progress)
        val addChild: View = view.findViewById(R.id.btn_add_child)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false))

    override fun onBindViewHolder(h: VH, position: Int) {
        val row = rows[position]
        val t = row.task
        val isGroup = project.hasChildren(t)
        val done = project.isDone(t)

        h.itemView.setPaddingRelative(h.itemView.context.dp(row.depth * 24), 0, 0, 0)

        h.arrow.visibility = if (isGroup) View.VISIBLE else View.INVISIBLE
        h.arrow.text = if (row.collapsed) "▸" else "▾"

        h.title.text = t.title
        h.title.setTypeface(null, if (isGroup) Typeface.BOLD else Typeface.NORMAL)
        val strike = done && !isGroup
        h.title.paintFlags = if (strike) h.title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        else h.title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

        h.check.isChecked = done
        h.check.isEnabled = !isGroup
        h.check.setOnClickListener { onToggle(t, h.check.isChecked) }

        h.progress.visibility = if (isGroup) View.VISIBLE else View.GONE
        if (isGroup) {
            val (d, n) = project.progress(t)
            h.progress.text = "$d/$n"
        }

        h.addChild.setOnClickListener { onAddChild(t) }
        h.itemView.setOnClickListener { if (isGroup) onArrow(t) }
        h.itemView.setOnLongClickListener {
            onDelete(t)
            true
        }
    }

    override fun getItemCount() = rows.size
}
