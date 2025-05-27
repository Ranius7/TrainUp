package com.rania.trainup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rania.trainup.databinding.ActivityActiveClientsTrainerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ActiveClientsTrainerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityActiveClientsTrainerBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sessionManager: SessionManager
    private lateinit var allClientAdapter: AllClientAdapter
    private val clientList = mutableListOf<Client>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActiveClientsTrainerBinding.inflate(layoutInflater)
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

        setSupportActionBar(binding.toolbarAllClients)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarAllClients.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }


        allClientAdapter = AllClientAdapter(clientList) { clickedClient ->
            navigateToClientDetail(clickedClient)
        }
        binding.rvAllActiveClients.apply {
            layoutManager = LinearLayoutManager(this@ActiveClientsTrainerActivity)
            adapter = allClientAdapter
        }

        loadClients(uid)
        setupBottomNavigationView()
    }

    private fun loadClients(trainerUid: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // ATENCIÓN: En Firestore tu campo es "new", no "isNew"
                val querySnapshot = firestore.collection("users")
                    .whereEqualTo("role", "CLIENT")
                    .whereEqualTo("trainerUid", trainerUid)
                    .whereEqualTo("new", false) // <-- CAMBIA "isNew" por "new"
                    .get()
                    .await()

                clientList.clear()
                for (doc in querySnapshot.documents) {
                    doc.toObject(Client::class.java)?.let { clientList.add(it) }
                }
                allClientAdapter.notifyDataSetChanged()
                if (clientList.isEmpty()) {
                    Toast.makeText(this@ActiveClientsTrainerActivity, "No tienes clientes activos asignados.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ActiveClientsTrainerActivity, "Error cargando clientes: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToClientDetail(client: Client) {
        val intent = Intent(this, ClientTrainerActivity::class.java)
        intent.putExtra("client_uid", client.uid)
        startActivity(intent)
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
                    true // Ya estamos aquí
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val (email, role, uid) = sessionManager.getSession()
        if (uid != null && role == MainActivity.ROLE_TRAINER) {
            loadClients(uid) // Refrescar clientes activos al volver
        }
    }
}