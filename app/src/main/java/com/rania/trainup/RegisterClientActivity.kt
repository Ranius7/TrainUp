package com.rania.trainup

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rania.trainup.databinding.ActivityRegisterClientBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterClientActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterClientBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val trainerEmails = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterClientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val objetivos = listOf("Perder peso", "Ganar masa muscular", "Mantener forma", "Mejorar resistencia")
        val objetivoAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, objetivos)
        objetivoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerObjetivo.adapter = objetivoAdapter

        loadTrainersForSpinner()

        binding.btnRegisterClient.setOnClickListener {
            registerClient()
        }

        binding.tvLoginRedirectClient.setOnClickListener {
            goToLogin()
        }
    }

    private fun loadTrainersForSpinner() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val querySnapshot = firestore.collection("users")
                    .whereEqualTo("role", "TRAINER")
                    .get()
                    .await()

                trainerEmails.clear()
                for (document in querySnapshot.documents) {
                    val email = document.getString("email")
                    email?.let { trainerEmails.add(it) }
                }

                if (trainerEmails.isEmpty()) {
                    trainerEmails.add("No hay entrenadores disponibles")
                    Toast.makeText(this@RegisterClientActivity, "No hay entrenadores registrados. Registra un entrenador primero.", Toast.LENGTH_LONG).show()
                }

                val adapter = ArrayAdapter(
                    this@RegisterClientActivity,
                    android.R.layout.simple_spinner_item,
                    trainerEmails
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerTrainerSelection.adapter = adapter

            } catch (e: Exception) {
                Toast.makeText(this@RegisterClientActivity, "Error cargando entrenadores: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun registerClient() {
        val name = binding.etNameClient.text.toString().trim()
        val email = binding.etEmailClient.text.toString().trim()
        val password = binding.etPasswordClient.text.toString().trim()
        val objective = binding.spinnerObjetivo.selectedItem.toString().trim()
        val selectedTrainerEmail = binding.spinnerTrainerSelection.selectedItem as? String

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || objective.isEmpty() ||
            selectedTrainerEmail.isNullOrEmpty() || selectedTrainerEmail == "No hay entrenadores disponibles") {
            Toast.makeText(this, "Completa todos los campos y selecciona un entrenador válido", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidEmail(email)) {
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
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                val trainerQuery = firestore.collection("users")
                                    .whereEqualTo("email", selectedTrainerEmail)
                                    .limit(1)
                                    .get().await()

                                val trainerUid = trainerQuery.documents.firstOrNull()?.id

                                if (trainerUid != null) {
                                    val client = Client(
                                        name = name,
                                        email = email,
                                        objective = objective,
                                        trainerUid = trainerUid,
                                        uid = uid,
                                        isNew = true,
                                        role = "CLIENT" // Siempre explícito
                                    )

                                    firestore.collection("users")
                                        .document(uid)
                                        .set(client)
                                        .addOnSuccessListener {
                                            Toast.makeText(this@RegisterClientActivity, "Cliente registrado con éxito", Toast.LENGTH_SHORT).show()
                                            goToLogin()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(this@RegisterClientActivity, "Error al guardar datos del cliente: ${e.message}", Toast.LENGTH_SHORT).show()
                                            user.delete()
                                        }
                                } else {
                                    Toast.makeText(this@RegisterClientActivity, "Error: No se encontró el entrenador seleccionado.", Toast.LENGTH_SHORT).show()
                                    user.delete()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this@RegisterClientActivity, "Error al buscar entrenador: ${e.message}", Toast.LENGTH_SHORT).show()
                                user.delete()
                            }
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
        intent.putExtra(LoginActivity.EXTRA_ROLE, MainActivity.ROLE_CLIENT)
        startActivity(intent)
        finish()
    }
}