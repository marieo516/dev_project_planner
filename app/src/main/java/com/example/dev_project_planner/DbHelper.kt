package com.example.dev_project_planner

import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteDatabase

class DbHelper(ctx: Context) : SQLiteOpenHelper(ctx, "planner.db", null, 1) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE projects (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                status TEXT NOT NULL DEFAULT 'Idea',
                start_date INTEGER,
                due_date INTEGER,
                brainstorm TEXT NOT NULL DEFAULT '',
                updated_at INTEGER NOT NULL DEFAULT 0
            )"""
        )
        db.execSQL(
            """CREATE TABLE labels (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                color INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE project_labels (
                project_id TEXT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                label_id TEXT NOT NULL REFERENCES labels(id) ON DELETE CASCADE,
                PRIMARY KEY (project_id, label_id)
            )"""
        )
        db.execSQL(
            """CREATE TABLE sprints (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                project_id TEXT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                name TEXT NOT NULL,
                start_date INTEGER NOT NULL,
                end_date INTEGER NOT NULL,
                position INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE images (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                project_id TEXT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                path TEXT NOT NULL,
                position INTEGER NOT NULL
            )"""
        )

        db.execSQL(
            """CREATE TABLE planning_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                project_id TEXT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                kind TEXT NOT NULL,
                text TEXT NOT NULL,
                position INTEGER NOT NULL
            )"""
        )

        db.execSQL(
            """CREATE TABLE tasks (
                id TEXT PRIMARY KEY,
                project_id TEXT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                parent_id TEXT REFERENCES tasks(id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED,
                title TEXT NOT NULL,
                done INTEGER NOT NULL DEFAULT 0,
                position INTEGER NOT NULL
            )"""
        )
        db.execSQL("CREATE INDEX idx_tasks_project ON tasks(project_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        listOf(
            "tasks", "planning_items", "images", "sprints",
            "project_labels", "labels", "projects"
        ).forEach { db.execSQL("DROP TABLE IF EXISTS $it") }
        onCreate(db)
    }
}
