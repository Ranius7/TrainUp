package com.rania.trainup

import android.content.Intent
import android.os.Bundle
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
        binding.toolbarSettings.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        if (currentUserRole == MainActivity.ROLE_CLIENT) {
            binding.bottomNavigationClient.visibility = View.VISIBLE
            binding.bottomNavigationTrainer.visibility = View.GONE
            setupBottomNavigationViewClient()
        } else { // Entrenador
            binding.bottomNavigationTrainer.visibility = View.VISIBLE
            binding.bottomNavigationClient.visibility = View.GONE
            setupBottomNavigationViewTrainer()
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnChangeName.setOnClickListener { showChangeNameDialog() }
        binding.btnChangeEmail.setOnClickListener { showChangeEmailDialog() }
        binding.btnChangePassword.setOnClickListener { showChangePasswordDialog() }
        binding.btnLogout.setOnClickListener { logout() }

//        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
//            Toast.makeText(this, "Notificaciones: $isChecked (no implementado)", Toast.LENGTH_SHORT).show()
//        }
//        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
//            Toast.makeText(this, "Modo oscuro: $isChecked (no implementado)", Toast.LENGTH_SHORT).show()
//        }
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

    private fun showChangeEmailDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Cambiar Email")
        val input = EditText(this)
        input.hint = "Nuevo correo electrónico"
        input.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        builder.setView(input)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val newEmail = input.text.toString().trim()
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                updateUserEmail(newEmail)
            } else {
                Toast.makeText(this, "Introduce un email válido", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun updateUserEmail(newEmail: String) {
        val user = auth.currentUser ?: return
        val uid = currentUserUid ?: return

        user.updateEmail(newEmail)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            firestore.collection("users").document(uid).update("email", newEmail).await()
                            sessionManager.saveSession(newEmail, currentUserRole!!, uid)
                            Toast.makeText(this@SettingsActivity, "Email actualizado.", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@SettingsActivity, "Error al actualizar email en DB: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this@SettingsActivity, "Error al cambiar email en Auth: ${authTask.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun showChangePasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Cambiar Contraseña")
        val input = EditText(this)
        input.hint = "Nueva contraseña (mínimo 6 caracteres)"
        input.inputType = android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val newPassword = input.text.toString().trim()
            if (newPassword.length >= 6) {
                updateUserPassword(newPassword)
            } else {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun updateUserPassword(newPassword: String) {
        val user = auth.currentUser ?: return

        user.updatePassword(newPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Contraseña actualizada.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error al cambiar contraseña: ${task.exception?.message}", Toast.LENGTH_LONG).show()
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
                    true
                }
                R.id.nav_settings -> {
                    true // Ya estamos aquí
                }
                else -> false
            }
        }
    }
}