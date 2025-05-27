package com.rania.trainup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rania.trainup.databinding.ActivityClientTrainerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ClientTrainerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClientTrainerBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sessionManager: SessionManager
    private var clientUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientTrainerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        sessionManager = SessionManager(this)

        val (email, role, uid) = sessionManager.getSession()

        if (uid == null || role != MainActivity.ROLE_TRAINER) {
            Toast.makeText(this, "Sesión no válida. Redirigiendo al inicio.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        clientUid = intent.getStringExtra("client_uid")

        if (clientUid == null) {
            Toast.makeText(this, "UID de cliente no proporcionado.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setSupportActionBar(binding.toolbarClientDetail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarClientDetail.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        loadClientDetails(clientUid!!)
        setupClickListeners()
        setupBottomNavigationView()
    }

    private fun loadClientDetails(uid: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val clientDoc = firestore.collection("users").document(uid).get().await()
                if (clientDoc.exists()) {
                    val client = clientDoc.toObject(Client::class.java)
                    client?.let {
                        binding.tvClientName.text = it.name.uppercase()
                        binding.tvClientInfo.text = it.objective // Muestra el objetivo, ya que la edad la quitamos

                        // Si el cliente es "nuevo" y el entrenador lo ve aquí,
                        // lo marcamos como "no nuevo" solo si viene de la lista de "nuevos clientes"
                        // Esto ya se gestiona en NewClientsTrainerActivity, aquí solo lo leemos.
                        if (it.isNew) {
                            // Opcional: Aquí podrías mostrar un aviso "Este es un cliente nuevo"
                        }
                    }
                } else {
                    Toast.makeText(this@ClientTrainerActivity, "Cliente no encontrado.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ClientTrainerActivity, "Error al cargar detalles del cliente: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupClickListeners() {
        binding.cardRoutine.setOnClickListener {
            val intent = Intent(this, RoutineTrainerActivity::class.java) // Asumo RoutineTrainerActivity
            intent.putExtra("client_uid", clientUid)
            startActivity(intent)
        }
        binding.cardProgress.setOnClickListener {
            val intent = Intent(this, ProgressTrainerActivity::class.java) // Asumo ProgressTrainerActivity
            intent.putExtra("client_uid", clientUid)
            startActivity(intent)
        }
    }

    private fun setupBottomNavigationView() {
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
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}