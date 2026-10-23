package com.example.dev_project_planner

import android.net.Uri
import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import java.io.File

class EditProjectActivity : AppCompatActivity() {

    private var project: Project? = null
    private val labelIds = mutableSetOf<String>()
    private val sprints = mutableListOf<Sprint>()
    private val images = mutableListOf<String>()
    private var start: Long? = null
    private var due: Long? = null
    private var sprintStart: Long? = null
    private var sprintEnd: Long? = null
    private var pendingPhoto: File? = null

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                images.add(copyToAppStorage(uri))
                renderImages()
            }
        }

    private val takePhoto =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
            val file = pendingPhoto
            if (file != null) {
                if (ok) {
                    images.add(file.absolutePath)
                    renderImages()
                } else {
                    file.delete()
                }
            }
            pendingPhoto = null
        }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchCamera()
            } else {
                Toast.makeText(this, ui(this, "Camera permission is needed to take a photo."), Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_project)
        applyInsets()

        val name = findViewById<TextInputEditText>(R.id.input_name)
        val desc = findViewById<TextInputEditText>(R.id.input_description)
        val statusGroup = findViewById<ChipGroup>(R.id.status_group)
        val startField = findViewById<TextInputEditText>(R.id.input_start)
        val dueField = findViewById<TextInputEditText>(R.id.input_due)
        val sprintName = findViewById<TextInputEditText>(R.id.input_sprint_name)
        val sprintStartField = findViewById<TextInputEditText>(R.id.input_sprint_start)
        val sprintEndField = findViewById<TextInputEditText>(R.id.input_sprint_end)
        val save = findViewById<MaterialButton>(R.id.btn_save)
        val delete = findViewById<MaterialButton>(R.id.btn_delete)
        val cancel = findViewById<MaterialButton>(R.id.btn_cancel)

        // Create or edit?
        project = Db.find(intent.getStringExtra(EXTRA_PROJECT_ID))
        val existing = project
        if (existing != null) {
            labelIds.addAll(existing.labelIds)
            sprints.addAll(existing.sprints)
            images.addAll(existing.images)
            start = existing.start
            due = existing.due
            name.setText(existing.name)
            desc.setText(existing.description)
            statusGroup.check(statusId(existing.status))
        } else {
            findViewById<TextView>(R.id.screen_title).text = ui(this, "New project")
            name.setText("")
            desc.setText("")
            statusGroup.check(R.id.status_idea)
            save.text = ui(this, "Create project")
            delete.visibility = View.GONE
            cancel.visibility = View.VISIBLE
            cancel.setOnClickListener { finish() }
        }
        startField.setText(start?.let { formatDate(it) } ?: "")
        dueField.setText(due?.let { formatDate(it) } ?: "")

        // Dates: tap to pick, long-press to clear
        startField.setOnClickListener {
            pickDate(supportFragmentManager, ui(this, "Start date")) {
                start = it
                startField.setText(formatDate(it))
            }
        }
        startField.setOnLongClickListener {
            start = null
            startField.setText("")
            true
        }
        dueField.setOnClickListener {
            pickDate(supportFragmentManager, ui(this, "Due date")) {
                due = it
                dueField.setText(formatDate(it))
            }
        }
        dueField.setOnLongClickListener {
            due = null
            dueField.setText("")
            true
        }

        // Sprints
        sprintStartField.setOnClickListener {
            pickDate(supportFragmentManager, ui(this, "Sprint start")) { d ->
                sprintStart = d
                sprintStartField.setText(formatDate(d))
            }
        }

        sprintEndField.setOnClickListener {
            pickDate(supportFragmentManager, ui(this, "Sprint end")) { d ->
                sprintEnd = d
                sprintEndField.setText(formatDate(d))
            }
        }

        findViewById<View>(R.id.btn_add_sprint).setOnClickListener {
            val n = sprintName.text.toString().trim()
            if (n.isEmpty()) {
                sprintName.error = ui(this, "Required")
                return@setOnClickListener
            }
            val sprintStartDate = sprintStart
            if (sprintStartDate == null) {
                Toast.makeText(this, ui(this, "Pick the sprint start date first"), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val sprintEndDate = sprintEnd
            if (sprintEndDate == null) {
                Toast.makeText(this, ui(this, "Pick the sprint end date first"), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sprints.add(Sprint(n, sprintStartDate, sprintEndDate))
            sprintName.setText("")
            sprintStartField.setText("")
            sprintEndField.setText("")
            sprintStart = null
            sprintEnd = null
            renderSprints()
        }

        // Images
        findViewById<View>(R.id.btn_upload).setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        findViewById<View>(R.id.btn_take_photo).setOnClickListener {
            if (
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PermissionChecker.PERMISSION_GRANTED
            ) {
                launchCamera()
            } else {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }

        // Save
        save.setOnClickListener {
            val n = name.text.toString().trim()
            if (n.isEmpty()) {
                name.error = ui(this, "Name is required")
                return@setOnClickListener
            }
            val target = project ?: Project(newId(), n).also { Db.add(it) }
            target.name = n
            target.description = desc.text.toString().trim()
            target.status = statusName(statusGroup.checkedChipId)
            target.start = start
            target.due = due
            target.labelIds.clear()
            target.labelIds.addAll(labelIds)
            target.sprints.clear()
            target.sprints.addAll(sprints)
            target.images.clear()
            target.images.addAll(images)
            Db.save(target)
            finish()
        }

        // Delete
        delete.setOnClickListener {
            val target = project ?: return@setOnClickListener
            confirm(
                this,
                "${ui(this, "Delete")} \"${target.name}\"? ${ui(this, "This can't be undone.")}",
                ui(this, "Delete")
            ) {
                Db.remove(target)
                finish()
            }
        }

        renderLabels()
        renderSprints()
        renderImages()
    }

    private fun renderLabels() {
        val group = findViewById<ChipGroup>(R.id.label_group)
        group.removeAllViews()
        for (label in Db.labels) {
            val tag = makeTag(this, group, label.name, selectable = true, dotColor = label.color)
            tag.isChecked = label.id in labelIds
            tag.setOnCheckedChangeListener { _, checked ->
                if (checked) labelIds.add(label.id) else labelIds.remove(label.id)
            }
            group.addView(tag)
        }
        val add = makeTag(this, group, ui(this, "+ Label"))
        add.setTextColor(ContextCompat.getColor(this, R.color.accent))
        add.setOnClickListener {
            showNewLabelDialog(this) { label ->
                labelIds.add(label.id)
                renderLabels()
            }
        }
        group.addView(add)
    }

    private fun renderSprints() {
        val list = findViewById<LinearLayout>(R.id.sprint_list)
        list.removeAllViews()
        for (s in sprints) {
            val row = layoutInflater.inflate(R.layout.item_sprint_edit, list, false)
            row.findViewById<TextView>(R.id.sprint_text).text =
                "${s.name} · ${formatDate(s.start)} – ${formatDate(s.end)}"
            row.findViewById<View>(R.id.btn_remove).setOnClickListener {
                sprints.remove(s)
                renderSprints()
            }
            list.addView(row)
        }
    }

    private fun renderImages() {
        val row = findViewById<LinearLayout>(R.id.image_row)
        row.removeAllViews()
        for (path in images) {
            val item = layoutInflater.inflate(R.layout.item_image, row, false)
            item.findViewById<ImageView>(R.id.image).setImageURI(Uri.fromFile(File(path)))
            item.findViewById<View>(R.id.btn_remove).setOnClickListener {
                images.remove(path)
                renderImages()
            }
            row.addView(item)
        }
    }

    private fun launchCamera() {
        val file = File(filesDir, "img_${System.currentTimeMillis()}.jpg")
        pendingPhoto = file
        try {
            takePhoto.launch(FileProvider.getUriForFile(this, "$packageName.fileprovider", file))
        } catch (_: android.content.ActivityNotFoundException) {
            pendingPhoto = null
            file.delete()
            Toast.makeText(this, ui(this, "No camera app is available on this device."), Toast.LENGTH_LONG).show()
        } catch (_: IllegalArgumentException) {
            pendingPhoto = null
            file.delete()
            Toast.makeText(this, ui(this, "Could not open the camera."), Toast.LENGTH_LONG).show()
        } catch (_: SecurityException) {
            pendingPhoto = null
            file.delete()
            Toast.makeText(this, ui(this, "Camera permission is needed to take a photo."), Toast.LENGTH_LONG).show()
        }
    }

    private fun copyToAppStorage(uri: Uri): String {
        val file = File(filesDir, "img_${System.currentTimeMillis()}.jpg")
        contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { out -> input.copyTo(out) }
        }
        return file.absolutePath
    }

    private fun statusId(status: String) = when (status) {
        "Planning" -> R.id.status_planning
        "In progress" -> R.id.status_in_progress
        "Completed" -> R.id.status_completed
        else -> R.id.status_idea
    }

    private fun statusName(id: Int) = when (id) {
        R.id.status_planning -> "Planning"
        R.id.status_in_progress -> "In progress"
        R.id.status_completed -> "Completed"
        else -> "Idea"
    }
}
