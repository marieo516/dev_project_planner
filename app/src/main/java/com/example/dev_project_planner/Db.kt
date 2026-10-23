package com.example.dev_project_planner

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.util.Calendar
import java.util.TimeZone
import androidx.core.database.sqlite.transaction

object Db {
    val labels = mutableListOf<Label>()
    val projects = mutableListOf<Project>()
    private lateinit var db: SQLiteDatabase

    fun init(ctx: Context) {
        if (::db.isInitialized) return
        db = DbHelper(ctx.applicationContext).writableDatabase
        load()
    }

    fun find(id: String?) = projects.firstOrNull { it.id == id }
    fun label(id: String) = labels.firstOrNull { it.id == id }

    // ---------- labels ----------

    fun addLabel(name: String, color: Int): Label {
        val label = Label(newId(), name, color)
        db.insert("labels", null, row("id" to label.id, "name" to name, "color" to color))
        labels.add(label)
        return label
    }

    fun removeLabel(label: Label) {
        db.transaction {
            delete("project_labels", "label_id = ?", arrayOf(label.id))
            delete("labels", "id = ?", arrayOf(label.id))
        }
        labels.removeAll { it.id == label.id }
        projects.forEach { it.labelIds.remove(label.id) }
    }

    // ---------- projects ----------

    fun add(p: Project) {
        projects.add(p)
    }

    fun remove(p: Project) {
        db.delete("projects", "id = ?", arrayOf(p.id))
        projects.remove(p)
    }

    fun save(p: Project) {
        p.updatedAt = System.currentTimeMillis()
        db.transaction { writeProject(p) }
    }

    private fun SQLiteDatabase.writeProject(p: Project) {
        val values = row(
            "id" to p.id, "name" to p.name, "description" to p.description,
            "status" to p.status, "start_date" to p.start, "due_date" to p.due,
            "brainstorm" to p.brainstorm, "updated_at" to p.updatedAt
        )
        if (update("projects", values, "id = ?", arrayOf(p.id)) == 0) {
            insert("projects", null, values)
        }

        val id = arrayOf(p.id)
        listOf("project_labels", "sprints", "images", "planning_items", "tasks")
            .forEach { delete(it, "project_id = ?", id) }

        p.labelIds.forEach {
            insert("project_labels", null, row("project_id" to p.id, "label_id" to it))
        }
        p.sprints.forEachIndexed { i, s ->
            insert(
                "sprints", null,
                row(
                    "project_id" to p.id, "name" to s.name, "start_date" to s.start,
                    "end_date" to s.end, "position" to i
                )
            )
        }
        p.images.forEachIndexed { i, path ->
            insert(
                "images", null,
                row("project_id" to p.id, "path" to path, "position" to i)
            )
        }
        fun items(kind: String, list: List<String>) = list.forEachIndexed { i, text ->
            insert(
                "planning_items", null,
                row("project_id" to p.id, "kind" to kind, "text" to text, "position" to i)
            )
        }
        items("tech", p.technologies)
        items("lib", p.libraries)
        items("feature", p.features)
        p.tasks.forEachIndexed { i, t ->
            insert(
                "tasks", null,
                row(
                    "id" to t.id, "project_id" to p.id, "parent_id" to t.parentId,
                    "title" to t.title, "done" to t.done, "position" to i
                )
            )
        }
    }

    fun replaceAll(newLabels: List<Label>, newProjects: List<Project>) {
        val oldLabels = labels.toList()
        val oldProjects = projects.toList()
        try {
            db.transaction {
                listOf(
                    "tasks", "planning_items", "images",
                    "sprints", "project_labels", "projects", "labels"
                )
                    .forEach { delete(it, null, null) }
                newLabels.forEach { label ->
                    insert("labels", null, row("id" to label.id, "name" to label.name, "color" to label.color))
                }
                newProjects.forEach { writeProject(it) }
            }
            labels.clear()
            labels.addAll(newLabels)
            projects.clear()
            projects.addAll(newProjects)
        } catch (error: Exception) {
            labels.clear()
            labels.addAll(oldLabels)
            projects.clear()
            projects.addAll(oldProjects)
            throw error
        }
    }

    fun saveBrainstorm(p: Project) {
        p.updatedAt = System.currentTimeMillis()
        db.execSQL(
            "UPDATE projects SET brainstorm = ?, updated_at = ? WHERE id = ?",
            arrayOf<Any?>(p.brainstorm, p.updatedAt, p.id)
        )
    }

    fun search(query: String, labelId: String?, sortBy: String): List<Project> {
        val sql = StringBuilder("SELECT p.id FROM projects p ")
        val args = mutableListOf<String>()
        if (labelId != null) {
            sql.append("JOIN project_labels pl ON pl.project_id = p.id AND pl.label_id = ? ")
            args.add(labelId)
        }
        sql.append("WHERE p.name LIKE ? ESCAPE '\\' ")
        args.add("%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%")
        sql.append(
            when (sortBy) {
                "Name" -> "ORDER BY p.name COLLATE NOCASE"
                "Status" -> "ORDER BY CASE p.status WHEN 'Idea' THEN 0 WHEN 'Planning' THEN 1 " +
                        "WHEN 'In progress' THEN 2 ELSE 3 END"

                else -> "ORDER BY (p.due_date IS NULL), p.due_date"
            }
        )
        val ids = mutableListOf<String>()
        db.rawQuery(sql.toString(), args.toTypedArray()).use { c ->
            while (c.moveToNext()) ids.add(c.getString(0))
        }
        return ids.mapNotNull { find(it) }
    }

    // ---------- tasks ----------

    fun insertTask(p: Project, title: String, parentId: String?): Task {
        val t = p.addTask(title, parentId)
        db.transaction {
            try {
                if (parentId != null) {
                    execSQL("UPDATE tasks SET done = 0 WHERE id = ?", arrayOf<Any?>(parentId))
                }
                execSQL(
                    "INSERT INTO tasks (id, project_id, parent_id, title, done, position) " +
                            "VALUES (?, ?, ?, ?, 0, " +
                            "(SELECT COALESCE(MAX(position), -1) + 1 FROM tasks WHERE project_id = ?))",
                    arrayOf<Any?>(t.id, p.id, parentId, title, p.id)
                )
                touch(p)
            } finally {
            }
        }
        return t
    }

    fun setTaskDone(p: Project, t: Task, done: Boolean) {
        t.done = done
        db.execSQL(
            "UPDATE tasks SET done = ? WHERE id = ?",
            arrayOf<Any?>(if (done) 1 else 0, t.id)
        )
        touch(p)
    }

    fun deleteTask(p: Project, t: Task) {
        p.removeTask(t)
        db.execSQL("DELETE FROM tasks WHERE id = ?", arrayOf<Any?>(t.id))
        touch(p)
    }

    private fun touch(p: Project) {
        p.updatedAt = System.currentTimeMillis()
        db.execSQL(
            "UPDATE projects SET updated_at = ? WHERE id = ?",
            arrayOf<Any?>(p.updatedAt, p.id)
        )
    }

    // ---------- Startup loading ----------

    private fun load() {
        labels.clear()
        projects.clear()

        db.rawQuery("SELECT id, name, color FROM labels ORDER BY rowid", null).use { c ->
            while (c.moveToNext()) labels.add(Label(c.getString(0), c.getString(1), c.getInt(2)))
        }
        db.rawQuery(
            "SELECT id, name, description, status, start_date, due_date, brainstorm, updated_at " +
                    "FROM projects ORDER BY rowid", null
        ).use { c ->
            while (c.moveToNext()) {
                val p = Project(c.getString(0), c.getString(1))
                p.description = c.getString(2) ?: ""
                p.status = c.getString(3) ?: "Idea"
                p.start = if (c.isNull(4)) null else c.getLong(4)
                p.due = if (c.isNull(5)) null else c.getLong(5)
                p.brainstorm = c.getString(6) ?: ""
                p.updatedAt = c.getLong(7)
                projects.add(p)
            }
        }
        projects.forEach { loadChildren(it) }
    }

    private fun loadChildren(p: Project) {
        val id = arrayOf(p.id)
        db.rawQuery("SELECT label_id FROM project_labels WHERE project_id = ?", id).use { c ->
            while (c.moveToNext()) p.labelIds.add(c.getString(0))
        }
        db.rawQuery(
            "SELECT name, start_date, end_date FROM sprints WHERE project_id = ? ORDER BY position",
            id
        ).use { c ->
            while (c.moveToNext()) p.sprints.add(Sprint(c.getString(0), c.getLong(1), c.getLong(2)))
        }
        db.rawQuery("SELECT path FROM images WHERE project_id = ? ORDER BY position", id).use { c ->
            while (c.moveToNext()) p.images.add(c.getString(0))
        }
        db.rawQuery(
            "SELECT kind, text FROM planning_items WHERE project_id = ? ORDER BY position", id
        ).use { c ->
            while (c.moveToNext()) {
                val text = c.getString(1)
                when (c.getString(0)) {
                    "tech" -> p.technologies.add(text)
                    "lib" -> p.libraries.add(text)
                    else -> p.features.add(text)
                }
            }
        }
        db.rawQuery(
            "SELECT id, parent_id, title, done FROM tasks WHERE project_id = ? ORDER BY position",
            id
        ).use { c ->
            while (c.moveToNext()) {
                val parent = if (c.isNull(1)) null else c.getString(1)
                p.tasks.add(Task(c.getString(0), c.getString(2), parent, c.getInt(3) == 1))
            }
        }
    }

    private fun row(vararg pairs: Pair<String, Any?>) = ContentValues().apply {
        for ((key, value) in pairs) when (value) {
            null -> putNull(key)
            is String -> put(key, value)
            is Int -> put(key, value)
            is Long -> put(key, value)
            is Boolean -> put(key, if (value) 1 else 0)
        }
    }
}
