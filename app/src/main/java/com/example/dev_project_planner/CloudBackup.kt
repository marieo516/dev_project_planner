package com.example.dev_project_planner

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object CloudBackup {
    private const val VERSION = 1
    private const val MAX_DOCUMENT_FIELD_BYTES = 700 * 1024
    private const val MAX_BACKUP_BYTES = 900 * 1024
    private const val WRITE_BATCH_SIZE = 400
    private val mainHandler = Handler(Looper.getMainLooper())

    private data class ImageSource(val id: String, val path: String)
    private data class ImageDocument(val id: String, val compressedBase64: String)
    private data class EncodedBackup(val backupId: String, val json: String, val images: List<ImageSource>)

    fun upload(uid: String, onResult: (Exception?) -> Unit) {
        val encoded = try {
            encode()
        } catch (error: Exception) {
            deliver(onResult, error)
            return
        }
        Thread {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val backupRef = firestore.collection("backups").document(uid)
                val oldSnapshot = Tasks.await(backupRef.get())
                val oldImageIds = oldSnapshot.getString("payload")?.let(::extractImageIds).orEmpty()

                val compressedImages = encoded.images.map { image ->
                    val compressed = gzip(File(image.path).readBytes())
                    require(compressed.size <= MAX_DOCUMENT_FIELD_BYTES) {
                        "An image is too large for Firestore after gzip compression."
                    }
                    ImageDocument(image.id, Base64.encodeToString(compressed, Base64.NO_WRAP))
                }
                compressedImages.chunked(WRITE_BATCH_SIZE).forEach { imageChunk ->
                    val batch = firestore.batch()
                    imageChunk.forEach { image ->
                        batch.set(
                            firestore.collection("images").document(image.id),
                            mapOf(
                                "ownerUid" to uid,
                                "backupId" to encoded.backupId,
                                "encoding" to "gzip+base64",
                                "data" to image.compressedBase64
                            )
                        )
                    }
                    Tasks.await(batch.commit())
                }

                Tasks.await(
                    backupRef.set(
                        mapOf(
                            "ownerUid" to uid,
                            "backupId" to encoded.backupId,
                            "version" to VERSION,
                            "payload" to encoded.json,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
                )

                try {
                    deleteImageDocuments(firestore, oldImageIds)
                } catch (_: Exception) {
                }
                deliver(onResult, null)
            } catch (error: Exception) {
                deliver(onResult, error)
            }
        }.start()
    }

    fun download(context: Context, uid: String, onResult: (Exception?) -> Unit) {
        Thread {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val backupSnapshot = Tasks.await(firestore.collection("backups").document(uid).get())
                require(backupSnapshot.exists()) { "No cloud backup was found for this account." }
                require(backupSnapshot.getString("ownerUid") == uid) { "Backup owner does not match this account." }
                val backupId = backupSnapshot.getString("backupId")
                    ?: throw IllegalArgumentException("The cloud backup is missing its ID.")
                val payload = backupSnapshot.getString("payload")
                    ?: throw IllegalArgumentException("The cloud backup is missing its data.")
                restore(context.applicationContext, uid, backupId, payload, firestore)
                deliver(onResult, null)
            } catch (error: Exception) {
                deliver(onResult, error)
            }
        }.start()
    }

    private fun encode(): EncodedBackup {
        val backupId = UUID.randomUUID().toString()
        val images = mutableListOf<ImageSource>()
        val root = JSONObject().put("version", VERSION).put("backupId", backupId)
        val labels = JSONArray()
        Db.labels.forEach { label ->
            labels.put(JSONObject().put("id", label.id).put("name", label.name).put("color", label.color))
        }
        root.put("labels", labels)

        val projects = JSONArray()
        Db.projects.forEach { project ->
            val item = JSONObject()
                .put("id", project.id)
                .put("name", project.name)
                .put("description", project.description)
                .put("status", project.status)
                .put("start", project.start ?: JSONObject.NULL)
                .put("due", project.due ?: JSONObject.NULL)
                .put("updatedAt", project.updatedAt)
                .put("brainstorm", project.brainstorm)
            item.put("labelIds", JSONArray().also { array -> project.labelIds.forEach(array::put) })
            item.put(
                "sprints",
                JSONArray().also { array ->
                    project.sprints.forEach { sprint ->
                        array.put(
                            JSONObject()
                                .put("name", sprint.name)
                                .put("start", sprint.start)
                                .put("end", sprint.end)
                        )
                    }
                }
            )
            item.put(
                "images",
                JSONArray().also { refs ->
                    project.images.forEach { path ->
                        val file = File(path)
                        require(file.isFile && file.canRead()) {
                            "A project image is missing or unreadable."
                        }
                        val id = "${backupId}_${UUID.randomUUID()}"
                        refs.put(id)
                        images.add(ImageSource(id, path))
                    }
                }
            )
            item.put("technologies", stringArray(project.technologies))
            item.put("libraries", stringArray(project.libraries))
            item.put("features", stringArray(project.features))
            item.put(
                "tasks",
                JSONArray().also { array ->
                    project.tasks.forEach { task ->
                        array.put(
                            JSONObject()
                                .put("id", task.id)
                                .put("title", task.title)
                                .put("parentId", task.parentId ?: JSONObject.NULL)
                                .put("done", task.done)
                        )
                    }
                }
            )
            projects.put(item)
        }
        root.put("projects", projects)
        val json = root.toString()
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_BACKUP_BYTES) {
            "Project data is too large for one Firestore backup document."
        }
        return EncodedBackup(backupId, json, images)
    }

    private fun restore(context: Context, uid: String, backupId: String, json: String, firestore: FirebaseFirestore) {
        val root = JSONObject(json)
        require(root.getInt("version") == VERSION) { "This backup version is not supported." }
        require(root.getString("backupId") == backupId) { "The cloud backup is inconsistent." }

        val labels = mutableListOf<Label>()
        val labelIds = mutableSetOf<String>()
        root.getJSONArray("labels").forEachObject { item ->
            val id = item.getString("id")
            require(labelIds.add(id)) { "The backup contains duplicate labels." }
            labels.add(Label(id, item.getString("name"), item.getInt("color")))
        }

        val projects = mutableListOf<Project>()
        val projectIds = mutableSetOf<String>()
        val allTaskIds = mutableSetOf<String>()
        root.getJSONArray("projects").forEachObject { item ->
            val id = item.getString("id")
            require(projectIds.add(id)) { "The backup contains duplicate projects." }
            val project = Project(id, item.getString("name")).apply {
                description = item.optString("description", "")
                status = item.optString("status", "Idea")
                start = item.longOrNull("start")
                due = item.longOrNull("due")
                updatedAt = item.optLong("updatedAt", 0L)
                brainstorm = item.optString("brainstorm", "")
            }
            item.getJSONArray("labelIds").forEachString { labelId ->
                require(labelId in labelIds) { "The backup references a missing label." }
                project.labelIds.add(labelId)
            }
            item.getJSONArray("sprints").forEachObject { sprint ->
                project.sprints.add(Sprint(sprint.getString("name"), sprint.getLong("start"), sprint.getLong("end")))
            }
            item.getJSONArray("images").forEachString { imageId ->
                val imageSnapshot = Tasks.await(firestore.collection("images").document(imageId).get())
                require(imageSnapshot.exists() && imageSnapshot.getString("ownerUid") == uid) {
                    "A backup image is missing or belongs to another account."
                }
                require(
                    imageSnapshot.getString("backupId") == backupId &&
                        imageSnapshot.getString("encoding") == "gzip+base64"
                ) {
                    "A backup image has invalid metadata."
                }
                val encoded = imageSnapshot.getString("data")
                    ?: throw IllegalArgumentException("A backup image has no data.")
                val imageBytes = gunzip(Base64.decode(encoded, Base64.DEFAULT))
                val file = File(context.filesDir, "img_restore_${UUID.randomUUID()}.img")
                file.writeBytes(imageBytes)
                project.images.add(file.absolutePath)
            }
            project.technologies.addAll(item.getJSONArray("technologies").stringList())
            project.libraries.addAll(item.getJSONArray("libraries").stringList())
            project.features.addAll(item.getJSONArray("features").stringList())
            item.getJSONArray("tasks").forEachObject { task ->
                project.tasks.add(
                    Task(
                        task.getString("id"),
                        task.getString("title"),
                        task.stringOrNull("parentId"),
                        task.optBoolean("done")
                    )
                )
            }
            val taskIds = project.tasks.map { it.id }.toSet()
            require(taskIds.size == project.tasks.size && project.tasks.all { allTaskIds.add(it.id) }) {
                "The backup contains duplicate task IDs."
            }
            require(project.tasks.all { it.parentId == null || it.parentId in taskIds }) {
                "The backup contains invalid task relationships."
            }
            val parents = project.tasks.associate { it.id to it.parentId }
            project.tasks.forEach { task ->
                val seen = mutableSetOf(task.id)
                var parentId = task.parentId
                while (parentId != null) {
                    require(seen.add(parentId)) { "The backup contains a circular task relationship." }
                    parentId = parents[parentId]
                }
            }
            projects.add(project)
        }
        Db.replaceAll(labels, projects)
    }

    private fun deleteImageDocuments(firestore: FirebaseFirestore, imageIds: List<String>) {
        imageIds.distinct().chunked(WRITE_BATCH_SIZE).forEach { ids ->
            val batch = firestore.batch()
            ids.forEach { id -> batch.delete(firestore.collection("images").document(id)) }
            Tasks.await(batch.commit())
        }
    }

    private fun extractImageIds(payload: String): List<String> = try {
        buildList {
            JSONObject(payload).getJSONArray("projects").forEachObject { project ->
                project.getJSONArray("images").forEachString(::add)
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun gzip(bytes: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { it.write(bytes) }
        return output.toByteArray()
    }

    private fun gunzip(bytes: ByteArray): ByteArray =
        GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }

    private fun stringArray(values: List<String>) = JSONArray().also { array -> values.forEach(array::put) }
    private fun JSONArray.forEachString(block: (String) -> Unit) { for (i in 0 until length()) block(getString(i)) }
    private fun JSONArray.forEachObject(block: (JSONObject) -> Unit) {
        for (i in 0 until length()) block(getJSONObject(i))
    }
    private fun JSONArray.stringList() = buildList { forEachString(::add) }
    private fun JSONObject.longOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)
    private fun JSONObject.stringOrNull(key: String): String? = if (isNull(key)) null else getString(key)

    private fun deliver(callback: (Exception?) -> Unit, error: Exception?) {
        mainHandler.post { callback(error) }
    }
}
