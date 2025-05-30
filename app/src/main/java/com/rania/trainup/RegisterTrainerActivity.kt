package com.rania.trainup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rania.trainup.databinding.ActivityRegisterTrainerBinding

class RegisterTrainerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterTrainerBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterTrainerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        binding.btnRegisterTrainer.setOnClickListener {
            registerTrainer()
        }

        binding.tvLoginRedirectTrainer.setOnClickListener {
            goToLogin()
        }
    }

    private fun registerTrainer() {
        val name = binding.etNameTrainer.text.toString().trim()
        val email = binding.etEmailTrainer.text.toString().trim()
        val password = binding.etPasswordTrainer.text.toString().trim()
        val specialty = binding.etSpecialty.text.toString().trim()
        val maxClients = binding.etMaxClientes.text.toString().trim().toIntOrNull()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || maxClients == null || maxClients <= 0 || specialty.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos correctamente", Toast.LENGTH_SHORT).show()
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Por favor, introduce un email válido", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid

                    if (uid != null) {
                        val trainer = hashMapOf(
                            "name" to name,
                            "email" to email,
                            "maxClients" to maxClients,
                            "specialty" to specialty,
                            "uid" to uid,
                            "role" to MainActivity.ROLE_TRAINER
                        )
                        firestore.collection("users")
                            .document(uid)
                            .set(trainer)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Entrenador registrado con éxito", Toast.LENGTH_SHORT).show()
                                goToLogin()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error al guardar datos del entrenador: ${e.message}", Toast.LENGTH_SHORT).show()
                                user.delete()
                            }
                    } else {
                        Toast.makeText(this, "Error al obtener UID del usuario.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Error al crear cuenta: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }



    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra(LoginActivity.EXTRA_ROLE, MainActivity.ROLE_TRAINER)
        startActivity(intent)
        finish()
    }
}