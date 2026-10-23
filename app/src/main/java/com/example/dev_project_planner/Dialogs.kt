package com.example.dev_project_planner

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.fragment.app.FragmentManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun confirm(ctx: Context, message: String, positive: String, onYes: () -> Unit) {
    MaterialAlertDialogBuilder(ctx)
        .setMessage(message)
        .setNegativeButton(ui(ctx, "Cancel"), null)
        .setPositiveButton(ui(ctx, positive)) { _, _ -> onYes() }
        .show()
}

fun Context.askText(
    title: String, hintText: String, initial: String = "", positive: String = "Add",
    onOk: (String) -> Unit
) {
    val input = EditText(this).apply {
        hint = hintText
        setText(initial)
        setSingleLine()
        setSelectAllOnFocus(true)
    }
    val box = FrameLayout(this).apply {
        setPadding(dp(24), dp(8), dp(24), 0)
        addView(input)
    }
    val dialog = MaterialAlertDialogBuilder(this)
        .setTitle(title)
        .setView(box)
        .setNegativeButton(ui(this, "Cancel"), null)
        .setPositiveButton(ui(this, positive)) { _, _ ->
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) onOk(text)
        }
        .create()
    dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    dialog.show()
    input.requestFocus()
}

fun pickDate(fm: FragmentManager, title: String, onPicked: (Long) -> Unit) {
    val picker = MaterialDatePicker.Builder.datePicker().setTitleText(title).build()
    picker.addOnPositiveButtonClickListener { onPicked(it) }
    picker.show(fm, "date")
}

fun showNewLabelDialog(ctx: Context, onAdded: (Label) -> Unit) {
    val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_new_label, null)
    val input = view.findViewById<EditText>(R.id.input_label_name)
    val row = view.findViewById<LinearLayout>(R.id.color_row)
    var selected = 0

    fun highlight() {
        for (i in 0 until row.childCount) row.getChildAt(i).alpha = if (i == selected) 1f else 0.35f
    }
    for (i in 0 until row.childCount) {
        val circle = row.getChildAt(i)
        circle.backgroundTintList = ColorStateList.valueOf(LABEL_COLORS[i])
        circle.setOnClickListener {
            selected = i
            highlight()
        }
    }
    highlight()

    MaterialAlertDialogBuilder(ctx)
        .setTitle(ui(ctx, "New label"))
        .setView(view)
        .setNegativeButton(ui(ctx, "Cancel"), null)
        .setPositiveButton(ui(ctx, "Add label")) { _, _ ->
            val name = input.text.toString().trim()
            if (name.isNotEmpty()) onAdded(Db.addLabel(name, LABEL_COLORS[selected]))
        }
        .show()
}
