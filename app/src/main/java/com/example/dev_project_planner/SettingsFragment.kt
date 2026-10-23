package com.example.dev_project_planner

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.button.MaterialButton
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private var authListener: FirebaseAuth.AuthStateListener? = null
    private var backupInProgress = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val labelsList = view.findViewById<LinearLayout>(R.id.labels_list)
        val emptyLabels = view.findViewById<TextView>(R.id.labels_empty)
        fun renderLabels() {
            labelsList.removeAllViews()
            emptyLabels.visibility = if (Db.labels.isEmpty()) View.VISIBLE else View.GONE
            Db.labels.forEach { label ->
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                row.addView(
                    makeTag(
                        requireContext(), labelsList, label.name,
                        dotColor = label.color, clickable = false
                    )
                )
                val remove = MaterialButton(requireContext()).apply {
                    text = ui(requireContext(), "Remove")
                    contentDescription = "${ui(requireContext(), "Remove label")} ${label.name}"
                    setOnClickListener {
                        val message = "${ui(requireContext(), "Delete label")} \"${label.name}\"? " +
                            ui(requireContext(), "This also removes it from projects.")
                        confirm(requireContext(), message, ui(requireContext(), "Delete")) {
                            Db.removeLabel(label)
                            renderLabels()
                        }
                    }
                }
                row.addView(remove)
                labelsList.addView(row)
            }
        }
        view.findViewById<View>(R.id.btn_add_label).setOnClickListener {
            showNewLabelDialog(requireContext()) { renderLabels() }
        }
        renderLabels()

        val status = view.findViewById<TextView>(R.id.cloud_status)
        val authButton = view.findViewById<MaterialButton>(R.id.btn_auth)
        val backupButton = view.findViewById<MaterialButton>(R.id.btn_backup)
        val restoreButton = view.findViewById<MaterialButton>(R.id.btn_restore)

        fun updateAuthUi(user: FirebaseUser?) {
            if (!isAdded) return
            status.text = user?.email?.let { "${ui(requireContext(), "Signed in as")} $it" }
                ?: ui(requireContext(), "Signed out")
            authButton.text = ui(requireContext(), if (user == null) "Sign in" else "Sign out")
            backupButton.isEnabled = user != null && !backupInProgress
            restoreButton.isEnabled = user != null && !backupInProgress
        }
        authListener = FirebaseAuth.AuthStateListener { updateAuthUi(it.currentUser) }.also(auth::addAuthStateListener)
        authButton.setOnClickListener {
            if (auth.currentUser != null) {
                auth.signOut()
            } else {
                showAuthChoice()
            }
        }
        backupButton.setOnClickListener {
            val user = auth.currentUser ?: return@setOnClickListener
            setBackupBusy(true, status, authButton, backupButton, restoreButton)
            CloudBackup.upload(user.uid) { error ->
                if (!isAdded) return@upload
                setBackupBusy(false, status, authButton, backupButton, restoreButton)
                toast(if (error == null) ui(requireContext(), "Backup complete.") else backupError(error))
            }
        }
        restoreButton.setOnClickListener {
            val user = auth.currentUser ?: return@setOnClickListener
            confirm(
                requireContext(),
                ui(requireContext(), "Restoring replaces all projects and labels on this device."),
                ui(requireContext(), "Restore")
            ) {
                setBackupBusy(true, status, authButton, backupButton, restoreButton)
                CloudBackup.download(requireContext().applicationContext, user.uid) { error ->
                    if (!isAdded) return@download
                    setBackupBusy(false, status, authButton, backupButton, restoreButton)
                    toast(if (error == null) ui(requireContext(), "Restore complete.") else backupError(error))
                }
            }
        }
        updateAuthUi(auth.currentUser)

        val languageGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.language_group)
        languageGroup.check(
            if (prefs.getString("language", "en") == "fr") {
                R.id.btn_language_fr
            } else {
                R.id.btn_language_en
            }
        )
        languageGroup.addOnButtonCheckedListener { _, id, checked ->
            if (!checked) return@addOnButtonCheckedListener
            val language = if (id == R.id.btn_language_fr) "fr" else "en"
            if (prefs.getString("language", "en") != language) {
                prefs.edit().putString("language", language).apply()
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
            }
        }

        // Theme
        val group = view.findViewById<MaterialButtonToggleGroup>(R.id.theme_group)
        group.check(
            when (prefs.getString("theme", "system")) {
                "light" -> R.id.btn_light
                "dark" -> R.id.btn_dark
                else -> R.id.btn_system
            }
        )
        group.addOnButtonCheckedListener { _, id, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val key = when (id) {
                R.id.btn_light -> "light"
                R.id.btn_dark -> "dark"
                else -> "system"
            }
            prefs.edit().putString("theme", key).apply()
            applySavedTheme(requireContext())
        }
    }

    override fun onDestroyView() {
        authListener?.let(auth::removeAuthStateListener)
        authListener = null
        super.onDestroyView()
    }

    private fun setBackupBusy(
        busy: Boolean, status: TextView, authButton: MaterialButton,
        backupButton: MaterialButton, restoreButton: MaterialButton
    ) {
        backupInProgress = busy
        val email = auth.currentUser?.email
        status.text = when {
            busy -> ui(requireContext(), "Working…")
            email != null -> "${ui(requireContext(), "Signed in as")} $email"
            else -> ui(requireContext(), "Signed out")
        }
        authButton.isEnabled = !busy
        backupButton.isEnabled = !busy && auth.currentUser != null
        restoreButton.isEnabled = !busy && auth.currentUser != null
    }

    private fun showAuthChoice() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_auth_choice, null)
        val dialog = MaterialAlertDialogBuilder(requireContext()).setView(dialogView).create()
        dialogView.findViewById<View>(R.id.btn_choice_sign_in).setOnClickListener {
            dialog.dismiss()
            showCredentialDialog(createAccount = false)
        }
        dialogView.findViewById<View>(R.id.btn_choice_create).setOnClickListener {
            dialog.dismiss()
            showCredentialDialog(createAccount = true)
        }
        dialog.show()
    }

    private fun showCredentialDialog(createAccount: Boolean) {
        val layout = if (createAccount) R.layout.dialog_create_account else R.layout.dialog_sign_in
        val content = layoutInflater.inflate(layout, null)
        val email = content.findViewById<TextInputEditText>(
            if (createAccount) R.id.input_create_email else R.id.input_signin_email
        )
        val password = content.findViewById<TextInputEditText>(
            if (createAccount) R.id.input_create_password else R.id.input_signin_password
        )
        val errorView = content.findViewById<TextView>(if (createAccount) R.id.create_error else R.id.signin_error)
        val progress = content.findViewById<View>(if (createAccount) R.id.create_progress else R.id.signin_progress)
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(ui(requireContext(), if (createAccount) "Create account" else "Sign in"))
            .setView(content)
            .setNegativeButton(ui(requireContext(), "Cancel"), null)
            .setPositiveButton(ui(requireContext(), if (createAccount) "Create account" else "Sign in"), null)
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val emailValue = email.text?.toString()?.trim().orEmpty()
                val passwordValue = password.text?.toString().orEmpty()
                val confirmValue = if (createAccount) {
                    content.findViewById<TextInputEditText>(R.id.input_create_confirm)
                        .text?.toString().orEmpty()
                } else {
                    passwordValue
                }
                errorView.visibility = View.GONE
                when {
                    emailValue.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailValue).matches() -> {
                        errorView.text = ui(requireContext(), "Enter a valid email address.")
                        errorView.visibility = View.VISIBLE
                    }
                    passwordValue.isBlank() -> {
                        errorView.text = ui(requireContext(), "Enter your password.")
                        errorView.visibility = View.VISIBLE
                    }
                    createAccount && passwordValue.length < 8 -> {
                        errorView.text = ui(requireContext(), "Password must be at least 8 characters.")
                        errorView.visibility = View.VISIBLE
                    }
                    createAccount && passwordValue != confirmValue -> {
                        errorView.text = ui(requireContext(), "Passwords do not match.")
                        errorView.visibility = View.VISIBLE
                    }
                    else -> {
                        progress.visibility = View.VISIBLE
                        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
                        val operation = if (createAccount) {
                            auth.createUserWithEmailAndPassword(emailValue, passwordValue)
                        } else {
                            auth.signInWithEmailAndPassword(emailValue, passwordValue)
                        }
                        operation.addOnCompleteListener { task ->
                            if (!isAdded) return@addOnCompleteListener
                            progress.visibility = View.GONE
                            if (task.isSuccessful) {
                                dialog.dismiss()
                            } else {
                                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                                errorView.text = task.exception?.localizedMessage
                                    ?: ui(requireContext(), "Could not sign in. Check your details and try again.")
                                errorView.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
        }
        dialog.show()
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun backupError(error: Exception): String {
        val key = when {
            error.message?.contains("image is too large", ignoreCase = true) == true ->
                "An image is too large for Firestore after compression."
            error.message?.contains("Project data is too large", ignoreCase = true) == true ->
                "Project data is too large for one Firestore backup document."
            error.message?.contains("No cloud backup", ignoreCase = true) == true ->
                "No cloud backup was found for this account."
            else -> if (error is IllegalArgumentException) {
                error.message ?: "Backup failed."
            } else {
                "Cloud backup failed. Check your connection and Firestore rules."
            }
        }
        return ui(requireContext(), key)
    }
}
