package com.example.dev_project_planner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup

class ProjectAdapter(private val onClick: (Project) -> Unit) :
    RecyclerView.Adapter<ProjectAdapter.VH>() {

    private var items: List<Project> = emptyList()

    fun submit(list: List<Project>) {
        items = list
        notifyDataSetChanged()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.name)
        val status: TextView = view.findViewById(R.id.status)
        val labels: ChipGroup = view.findViewById(R.id.labels)
        val tasks: TextView = view.findViewById(R.id.tasks)
        val due: TextView = view.findViewById(R.id.due)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_project, parent, false))

    override fun onBindViewHolder(h: VH, position: Int) {
        val p = items[position]
        h.name.text = p.name
        h.status.text = ui(h.itemView.context, p.status)

        h.labels.removeAllViews()
        p.labelIds.mapNotNull { Db.label(it) }.forEach { label ->
            h.labels.addView(
                makeTag(h.itemView.context, h.labels, label.name, dotColor = label.color, clickable = false)
            )
        }

        val (done, total) = p.totalProgress()
        h.tasks.text = "$done/$total ${ui(h.itemView.context, "tasks")}"
        h.due.text = "${ui(h.itemView.context, "Due ")}${formatDate(p.due)}"
        h.itemView.setOnClickListener { onClick(p) }
    }

    override fun getItemCount() = items.size
}
