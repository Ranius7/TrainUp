package com.rania.trainup

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rania.trainup.databinding.ActivitySettingsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.EmailAuthProvider

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sessionManager: SessionManager
    private var currentUserUid: String? = null
    private var currentUserRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        sessionManager = SessionManager(this)

        val (email, role, uid) = sessionManager.getSession()
        currentUserUid = uid
        currentUserRole = role

        if (currentUserUid == null || currentUserRole == null) {
            Toast.makeText(this, "Sesión no válida. Redirigiendo al inicio.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setSupportActionBar(binding.toolbarSettings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "AJUSTES"
        binding.toolbarSettings.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        if (currentUserRole == MainActivity.ROLE_CLIENT) {
            binding.bottomNavigationClient.visibility = View.VISIBLE
            binding.bottomNavigationTrainer.visibility = View.GONE
            setupBottomNavigationViewClient()
        } else {
            binding.bottomNavigationTrainer.visibility = View.VISIBLE
            binding.bottomNavigationClient.visibility = View.GONE
            setupBottomNavigationViewTrainer()
        }
        binding.bottomNavigationTrainer.selectedItemId = R.id.nav_settings

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnChangeName.setOnClickListener { showChangeNameDialog() }
        binding.btnChangePassword.setOnClickListener { showChangePasswordDialog() }
        binding.btnLogout.setOnClickListener { logout() }
        binding.btnDeleteAccount.setOnClickListener { showDeleteAccountDialog() }
    }

    private fun showDeleteAccountDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirm_delete, null)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnDelete = dialogView.findViewById<Button>(R.id.btnDelete)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnDelete.setOnClickListener {
            showReauthDialogAndDeleteAccount()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showReauthDialogAndDeleteAccount() {
        showReauthDialog { currentPassword ->
            reauthenticateAndDeleteAccount(currentPassword)
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirma tu contraseña")
        val input = EditText(this)
        input.hint = "Contraseña actual"
        input.inputType = android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)

        builder.setPositiveButton("Confirmar") { dialog, _ ->
            val currentPassword = input.text.toString()
            reauthenticateAndDeleteAccount(currentPassword)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun reauthenticateAndDeleteAccount(currentPassword: String) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return
        val credential = EmailAuthProvider.getCredential(email, currentPassword)

        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    val uid = currentUserUid ?: return@addOnCompleteListener
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            firestore.collection("users").document(uid).delete().await()
                            user.delete()
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        sessionManager.clearSession()
                                        val intent =
                                            Intent(this@SettingsActivity, MainActivity::class.java)
                                        intent.flags =
                                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                        finish()
                                        Toast.makeText(
                                            this@SettingsActivity,
                                            "Cuenta eliminada.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            this@SettingsActivity,
                                            "Error al eliminar cuenta: ${task.exception?.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@SettingsActivity,
                                "Error al eliminar datos: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Reautenticación fallida: ${reauthTask.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }


    private fun showChangeNameDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_input_field, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etInput = dialogView.findViewById<EditText>(R.id.etDialogInput)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        tvTitle.text = "Cambiar nombre"
        etInput.hint = "Nuevo nombre"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val newName = etInput.text.toString().trim()
            if (newName.isNotEmpty()) {
                updateUserName(newName)
                dialog.dismiss()
            } else {
                etInput.error = "El nombre no puede estar vacío"
            }
        }
        dialog.show()
    }

    private fun updateUserName(newName: String) {
        val uid = currentUserUid ?: return
        CoroutineScope(Dispatchers.Main).launch {
            try {
                firestore.collection("users").document(uid).update("name", newName).await()
                Toast.makeText(this@SettingsActivity, "Nombre actualizado.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Error al cambiar nombre: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_input_field, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etInput = dialogView.findViewById<EditText>(R.id.etDialogInput)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        tvTitle.text = "Cambiar contraseña"
        etInput.hint = "Nueva contraseña (mínimo 6 caracteres)"
        etInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val newPassword = etInput.text.toString().trim()
            if (newPassword.length >= 6) {
                showReauthDialog { currentPassword ->
                    reauthenticateAndChangePassword(currentPassword, newPassword)
                }
                dialog.dismiss()
            } else {
                etInput.error = "La contraseña debe tener al menos 6 caracteres"
            }
        }
        dialog.show()
    }

    private fun showReauthDialog(onPasswordEntered: (String) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_input_field, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etPassword = dialogView.findViewById<EditText>(R.id.etDialogInput)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnSave)

        tvTitle.text = "Confirma tu contraseña"
        etPassword.hint = "Contraseña actual"
        etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        etPassword.text.clear()

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            val password = etPassword.text.toString()
            if (password.isNotEmpty()) {
                onPasswordEntered(password)
                dialog.dismiss()
            } else {
                etPassword.error = "Introduce tu contraseña"
            }
        }
        dialog.show()
    }

    private fun reauthenticateAndChangePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return
        val credential = EmailAuthProvider.getCredential(email, currentPassword)

        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Contraseña actualizada.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Reautenticación fallida: ${reauthTask.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }


    private fun logout() {
        auth.signOut()
        sessionManager.clearSession()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        Toast.makeText(this, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
    }

    private fun setupBottomNavigationViewClient() {
        binding.bottomNavigationClient.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.itNavHome -> {
                    startActivity(Intent(this, HomeClientActivity::class.java))
                    finish()
                    true
                }
                R.id.itNavTraining -> {
                    startActivity(Intent(this, RoutineClientActivity::class.java))
                    true
                }
                R.id.itNavProfile -> {
                    startActivity(Intent(this, ProfileClientActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupBottomNavigationViewTrainer() {
        binding.bottomNavigationTrainer.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeTrainerActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_clients -> {
                    startActivity(Intent(this, ActiveClientsTrainerActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    true
                }
                else -> false
            }
        }
    }
}