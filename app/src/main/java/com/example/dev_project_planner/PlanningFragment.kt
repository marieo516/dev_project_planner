package com.example.dev_project_planner

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText

class PlanningFragment : Fragment(R.layout.fragment_planning) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val p = currentProject() ?: return

        view.findViewById<TextView>(R.id.project_name).text = p.name
        view.findViewById<View>(R.id.btn_back).setOnClickListener { requireActivity().finish() }

        view.findViewById<View>(R.id.btn_add_tech).setOnClickListener {
            requireContext().askText(ui(requireContext(), "Add technology"), ui(requireContext(), "e.g. Kotlin")) {
                p.technologies.add(it)
                Db.save(p)
                render()
            }
        }
        view.findViewById<View>(R.id.btn_add_library).setOnClickListener {
            requireContext().askText(ui(requireContext(), "Add library"), ui(requireContext(), "e.g. Coil")) {
                p.libraries.add(it)
                Db.save(p)
                render()
            }
        }
        view.findViewById<View>(R.id.btn_add_feature).setOnClickListener {
            requireContext().askText(ui(requireContext(), "Add feature"), ui(requireContext(), "e.g. Dark mode")) {
                p.features.add(it)
                Db.save(p)
                render()
            }
        }

        val brain = view.findViewById<TextInputEditText>(R.id.input_brainstorm)
        brain.setText(p.brainstorm)
        brain.doAfterTextChanged { p.brainstorm = it?.toString().orEmpty() }

        render()
    }

    override fun onPause() {
        super.onPause()
        currentProject()?.let { Db.saveBrainstorm(it) }
    }

    private fun render() {
        val p = currentProject() ?: return
        val v = requireView()

        fillTags(v.findViewById(R.id.tech_group), p.technologies)
        fillTags(v.findViewById(R.id.library_group), p.libraries)

        val list = v.findViewById<LinearLayout>(R.id.feature_list)
        list.removeAllViews()
        p.features.toList().forEach { feature ->
            val row = layoutInflater.inflate(R.layout.item_feature, list, false)
            row.findViewById<TextView>(R.id.feature_text).text = feature
            row.findViewById<View>(R.id.btn_to_tasks).setOnClickListener {
                p.features.remove(feature)
                p.addTask(feature, null)
                Db.save(p)
                Toast.makeText(requireContext(), ui(requireContext(), "Moved to Tasks"), Toast.LENGTH_SHORT).show()
                render()
            }
            list.addView(row)
        }
    }

    private fun fillTags(group: ChipGroup, items: MutableList<String>) {
        group.removeAllViews()
        items.toList().forEach { item ->
            val tag = makeTag(requireContext(), group, item)
            tag.isCloseIconVisible = true
            tag.setOnCloseIconClickListener {
                items.remove(item)
                currentProject()?.let { Db.save(it) }
                render()
            }
            group.addView(tag)
        }
    }
}
