package com.example.dev_project_planner

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.material.chip.Chip

fun makeTag(
    ctx: Context, parent: ViewGroup, text: String,
    selectable: Boolean = false, dotColor: Int? = null, clickable: Boolean = true
): Chip {
    val layout = if (selectable) R.layout.chip_filter else R.layout.chip_assist
    val chip = LayoutInflater.from(ctx).inflate(layout, parent, false) as Chip
    chip.text = text
    if (dotColor != null) {
        chip.setChipIconResource(R.drawable.circle_color)
        chip.chipIconTint = ColorStateList.valueOf(dotColor)
        chip.isChipIconVisible = true
    }
    if (!selectable && !clickable) {
        chip.isClickable = false
        chip.isFocusable = false
    }
    return chip
}
