package com.rania.trainup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rania.trainup.databinding.ActivityLoginBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ROLE = "EXTRA_ROLE"
    }

    private lateinit var binding: ActivityLoginBinding
    private var selectedRole: String? = null // Cambiado a selectedRole para evitar confusión con el campo role de la DB
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        sessionManager = SessionManager(this)

        selectedRole = intent.getStringExtra(EXTRA_ROLE)

        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        binding.tvRegisterRedirect.setOnClickListener {
            navigateToRegister()
        }
    }

    private fun performLogin() {
        val email = binding.etEmailLogin.text.toString().trim()
        val password = binding.etPasswordLogin.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid

                    if (uid != null) {
                        // Validar que el rol del usuario en la DB coincide con el rol seleccionado
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                val userDoc = firestore.collection("users").document(uid).get().await()
                                val userRoleInDb = userDoc.getString("role")

                                if (userDoc.exists() && userRoleInDb == selectedRole) {
                                    sessionManager.saveSession(email, selectedRole!!, uid)
                                    Toast.makeText(this@LoginActivity, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                                    navigateToHome(selectedRole!!)
                                } else {
                                    auth.signOut() // Cerrar sesión si el rol no coincide o no existe
                                    Toast.makeText(this@LoginActivity, "Credenciales o rol incorrectos. Intenta de nuevo.", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                auth.signOut()
                                Toast.makeText(this@LoginActivity, "Error al verificar rol: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Error: UID de usuario no encontrado.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToRegister() {
        val intent = when (selectedRole) {
            MainActivity.ROLE_CLIENT -> Intent(this, RegisterClientActivity::class.java)
            MainActivity.ROLE_TRAINER -> Intent(this, RegisterTrainerActivity::class.java)
            else -> Intent(this, MainActivity::class.java) // En caso de rol desconocido, vuelve al main
        }
        intent.putExtra(EXTRA_ROLE, selectedRole)
        startActivity(intent)
    }

    private fun navigateToHome(role: String) {
        val intent = when (role) {
            MainActivity.ROLE_CLIENT -> Intent(this, HomeClientActivity::class.java)
            MainActivity.ROLE_TRAINER -> Intent(this, HomeTrainerActivity::class.java)
            else -> Intent(this, MainActivity::class.java) // En caso de rol desconocido
        }
        startActivity(intent)
        finish() // Cierra LoginActivity
    }
}