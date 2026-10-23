package com.example.dev_project_planner

import java.util.UUID

val STATUSES = listOf("Idea", "Planning", "In progress", "Completed")

fun newId(): String = UUID.randomUUID().toString()

class Sprint(val name: String, val start: Long, val end: Long)

class Project(val id: String, var name: String) {
    var description = ""
    var status = "Idea"
    var start: Long? = null
    var due: Long? = null
    var updatedAt = 0L
    var brainstorm = ""
    val labelIds = mutableSetOf<String>()
    val sprints = mutableListOf<Sprint>()
    val images = mutableListOf<String>()
    val technologies = mutableListOf<String>()
    val libraries = mutableListOf<String>()
    val features = mutableListOf<String>()
    val tasks = mutableListOf<Task>()

    // ----- task tree -----

    fun children(parentId: String?) = tasks.filter { it.parentId == parentId }
    fun hasChildren(t: Task) = tasks.any { it.parentId == t.id }

    fun isDone(t: Task): Boolean =
        if (hasChildren(t)) children(t.id).all { isDone(it) } else t.done

    private fun leaves(t: Task): List<Task> =
        if (!hasChildren(t)) listOf(t) else children(t.id).flatMap { leaves(it) }

    fun progress(t: Task): Pair<Int, Int> {
        val l = leaves(t)
        return l.count { it.done } to l.size
    }

    fun totalProgress(): Pair<Int, Int> {
        val all = children(null).flatMap { leaves(it) }
        return all.count { it.done } to all.size
    }

    fun addTask(title: String, parentId: String?): Task {
        tasks.firstOrNull { it.id == parentId }?.done = false
        val t = Task(newId(), title, parentId)
        tasks.add(t)
        return t
    }

    fun removeTask(t: Task) {
        children(t.id).forEach { removeTask(it) }
        tasks.remove(t)
    }
}
