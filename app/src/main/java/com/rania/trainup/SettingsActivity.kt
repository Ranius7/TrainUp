package com.rania.trainup

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
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

        // Toolbar igual que en el resto de pantallas y título en mayúsculas
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

        setupClickListeners()

    }

    private fun setupClickListeners() {
        binding.btnChangeName.setOnClickListener { showChangeNameDialog() }
        binding.btnChangePassword.setOnClickListener { showChangePasswordDialog() }
        binding.btnLogout.setOnClickListener { logout() }
        binding.btnDeleteAccount.setOnClickListener { showDeleteAccountDialog() }
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar cuenta")
            .setMessage("¿Seguro que quieres eliminar tu cuenta? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { dialog, _ ->
                showReauthDialogAndDeleteAccount()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showReauthDialogAndDeleteAccount() {
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
                    // Primero elimina de Firestore
                    val uid = currentUserUid ?: return@addOnCompleteListener
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            firestore.collection("users").document(uid).delete().await()
                            // Luego elimina de Auth
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
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Cambiar Nombre")
        val input = EditText(this)
        input.hint = "Nuevo nombre"
        builder.setView(input)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val newName = input.text.toString().trim()
            if (newName.isNotEmpty()) {
                updateUserName(newName)
            } else {
                Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
        builder.show()
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
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Cambiar Contraseña")
        val input = EditText(this)
        input.hint = "Nueva contraseña (mínimo 6 caracteres)"
        input.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val newPassword = input.text.toString().trim()
            if (newPassword.length >= 6) {
                showReauthDialogAndChangePassword(newPassword)
            } else {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun showReauthDialogAndChangePassword(newPassword: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Introduce tu contraseña actual")
        val input = EditText(this)
        input.hint = "Contraseña actual"
        input.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)

        builder.setPositiveButton("Confirmar") { dialog, _ ->
            val currentPassword = input.text.toString()
            reauthenticateAndChangePassword(currentPassword, newPassword)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
        builder.show()
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