package com.rania.trainup

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.rania.trainup.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        const val ROLE_CLIENT = "CLIENT"
        const val ROLE_TRAINER = "TRAINER"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        auth = FirebaseAuth.getInstance()

        // Comprobar si ya hay una sesión iniciada
        val (email, role, uid) = sessionManager.getSession()
        val firebaseUser = auth.currentUser

        if (firebaseUser != null && email != null && role != null && uid == firebaseUser.uid) {
            // Si el usuario está autenticado en Firebase y tenemos su sesión guardada, redirigimos directamente
            navigateToHome(role)
            return // Salir de onCreate para evitar inflar la vista de selección de rol
        }

        // Si no hay sesión o no coincide con Firebase Auth, mostramos la pantalla de selección de rol
        binding.btnClient.setOnClickListener {
            navigateToLogin(ROLE_CLIENT)
        }

        binding.btnTrainer.setOnClickListener {
            navigateToLogin(ROLE_TRAINER)
        }
    }

    private fun navigateToLogin(role: String) {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra(LoginActivity.EXTRA_ROLE, role)
        startActivity(intent)
    }

    private fun navigateToHome(role: String) {
        val intent = when (role) {
            ROLE_CLIENT -> Intent(this, HomeClientActivity::class.java)
            ROLE_TRAINER -> Intent(this, HomeTrainerActivity::class.java)
            else -> null
        }
        intent?.let {
            startActivity(it)
            finish() // Cierra MainActivity para que el usuario no pueda volver con el botón atrás
        }
    }
}