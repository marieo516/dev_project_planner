package com.example.dev_project_planner

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun exportProjectSummary(context: Context, project: Project): String {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
    val filename = "${safeFileName(project.name)}_summary_$timestamp.md"
    val content = buildProjectSummary(context, project)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/markdown")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Project Planner")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create export file")
        try {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                it.write(content)
            } ?: error("Could not open export file")
            context.contentResolver.update(
                uri,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                null,
                null
            )
        } catch (error: Exception) {
            context.contentResolver.delete(uri, null, null)
            throw error
        }
        return "Downloads/Project Planner/$filename"
    }

    val directory = File(
        context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
        "Project Planner"
    )
    check(directory.exists() || directory.mkdirs()) { "Could not create export folder" }
    val file = File(directory, filename)
    file.writeText(content, Charsets.UTF_8)
    return file.absolutePath
}

private fun buildProjectSummary(context: Context, project: Project): String = buildString {
    appendLine("# ${project.name}")
    appendLine()
    appendLine("## ${ui(context, "Overview")}")
    appendLine()
    appendLine("- **${ui(context, "Status")}:** ${ui(context, project.status)}")
    if (project.description.isNotBlank()) appendLine("- **${ui(context, "Description")}:** ${project.description}")
    val labelNames = project.labelIds.mapNotNull { Db.label(it)?.name }
    if (labelNames.isNotEmpty()) appendLine("- **${ui(context, "Labels")}:** ${labelNames.joinToString(", ")}")
    project.start?.let { appendLine("- **${ui(context, "Start date")}:** ${formatDate(it)}") }
    project.due?.let { appendLine("- **${ui(context, "Due date")}:** ${formatDate(it)}") }
    if (project.sprints.isNotEmpty()) {
        appendLine("- **${ui(context, "Sprints")}:**")
        project.sprints.forEach { appendLine("  - ${it.name}: ${formatDate(it.start)} – ${formatDate(it.end)}") }
    }

    appendLine()
    appendLine("## ${ui(context, "Planning")}")
    appendLine()
    appendList(context, "Technologies", project.technologies)
    appendList(context, "Libraries", project.libraries)
    appendLine("### ${ui(context, "Brainstorming")}")
    appendLine()
    appendLine(project.brainstorm.ifBlank { "—" })
    appendLine()
    appendList(context, "Future features", project.features)
}

private fun StringBuilder.appendList(context: Context, title: String, values: List<String>) {
    appendLine("### ${ui(context, title)}")
    appendLine()
    if (values.isEmpty()) appendLine("—") else values.forEach { appendLine("- $it") }
    appendLine()
}

private fun safeFileName(name: String): String = name
    .trim()
    .replace(Regex("[\\\\/:*?\"<>|]"), "_")
    .trim('.', ' ')
    .ifBlank { "project" }
