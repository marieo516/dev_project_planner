package com.example.dev_project_planner

class Task(
    val id: String,
    var title: String,
    val parentId: String?,
    var done: Boolean = false
)
