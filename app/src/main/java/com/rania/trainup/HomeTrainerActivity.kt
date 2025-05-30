package com.rania.trainup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rania.trainup.databinding.ActivityHomeTrainerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeTrainerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeTrainerBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sessionManager: SessionManager
    private lateinit var newClientsAdapter: NewClientAdapter
    private val newClientsList = mutableListOf<Client>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeTrainerBinding.inflate(layoutInflater)
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

        newClientsAdapter = NewClientAdapter(newClientsList) { clickedClient ->
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    firestore.collection("users").document(clickedClient.uid).update("new", false).await()
                    val index = newClientsList.indexOfFirst { it.uid == clickedClient.uid }
                    if (index != -1) {
                        newClientsList.removeAt(index)
                        newClientsAdapter.notifyItemRemoved(index)
                    }
                    binding.cvNewClients.visibility = if (newClientsList.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE

                    navigateToClientDetail(clickedClient)
                } catch (e: Exception) {
                    Toast.makeText(this@HomeTrainerActivity, "Error al actualizar estado de cliente: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.rvNewClients.apply {
            layoutManager = LinearLayoutManager(this@HomeTrainerActivity)
            adapter = newClientsAdapter
        }

        loadTrainerData(uid)
        setupBottomNavigationView()
        setupClickListeners()
    }

    private fun loadTrainerData(uid: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val documentSnapshot = firestore.collection("users").document(uid).get().await()
                if (documentSnapshot.exists()) {
                    val trainer = documentSnapshot.toObject(Trainer::class.java)
                    binding.tvWelcomeTrainer.text = "👋 ¡Hola, ${trainer?.name ?: "Entrenador"}!"
                    binding.tvDateTrainer.text = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("es", "ES")).format(Date()).replaceFirstChar { it.titlecase(Locale("es", "ES")) }

                    loadNewClients(uid)
                } else {
                    Toast.makeText(this@HomeTrainerActivity, "Datos de entrenador no encontrados.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@HomeTrainerActivity, "Error al cargar datos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadNewClients(trainerUid: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val querySnapshot = firestore.collection("users")
                    .whereEqualTo("role", "CLIENT")
                    .whereEqualTo("trainerUid", trainerUid)
                    .whereEqualTo("new", true) // <-- CAMBIA "isNew" por "new"
                    .get()
                    .await()

                newClientsList.clear()
                for (doc in querySnapshot.documents) {
                    doc.toObject(Client::class.java)?.let { newClientsList.add(it) }
                }
                newClientsAdapter.notifyDataSetChanged()
                binding.cvNewClients.visibility = if (newClientsList.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
            } catch (e: Exception) {
                Toast.makeText(this@HomeTrainerActivity, "Error cargando nuevos clientes: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBottomNavigationView() {
        binding.bottomNavigationTrainer.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    true // Ya estamos en Home
                }
                R.id.nav_clients -> {
                    startActivity(Intent(this, ActiveClientsTrainerActivity::class.java))
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

    private fun setupClickListeners() {
        binding.cvNewClients.setOnClickListener {
            val intent = Intent(this, NewClientsTrainerActivity::class.java)
            startActivity(intent)
        }
        binding.cvManageClients.setOnClickListener {
            val intent = Intent(this, ActiveClientsTrainerActivity::class.java)
            startActivity(intent)
        }
    }

    private fun navigateToClientDetail(client: Client) {
        val intent = Intent(this, ClientTrainerActivity::class.java)
        intent.putExtra("client_uid", client.uid)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        val (email, role, uid) = sessionManager.getSession()
        if (uid != null && role == MainActivity.ROLE_TRAINER) {
            loadNewClients(uid) // Refrescar nuevos clientes al volver
        }
    }
}