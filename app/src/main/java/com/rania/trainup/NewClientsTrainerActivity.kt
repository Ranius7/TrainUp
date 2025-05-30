package com.rania.trainup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rania.trainup.databinding.ActivityNewClientsTrainerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NewClientsTrainerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewClientsTrainerBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sessionManager: SessionManager
    private lateinit var newClientsAdapter: NewClientAdapter
    private val newClientsList = mutableListOf<Client>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewClientsTrainerBinding.inflate(layoutInflater)
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

        setSupportActionBar(binding.toolbarNewClients)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "NUEVOS CLIENTES"
        binding.toolbarNewClients.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        newClientsAdapter = NewClientAdapter(newClientsList) { clickedClient ->
            navigateToClientDetail(clickedClient)
        }
        binding.rvNewClients.apply {
            layoutManager = LinearLayoutManager(this@NewClientsTrainerActivity)
            adapter = newClientsAdapter
        }

        binding.bottomNavigationTrainer.selectedItemId = R.id.nav_clients


        loadNewClients(uid)
        setupBottomNavigationView()
    }

    private fun loadNewClients(trainerUid: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val querySnapshot = firestore.collection("users")
                    .whereEqualTo("role", "CLIENT")
                    .whereEqualTo("trainerUid", trainerUid)
                    .whereEqualTo("new", true)
                    .get()
                    .await()

                newClientsList.clear()
                for (doc in querySnapshot.documents) {
                    doc.toObject(Client::class.java)?.let { newClientsList.add(it) }
                }
                newClientsAdapter.notifyDataSetChanged()
                if (newClientsList.isEmpty()) {
                    Toast.makeText(this@NewClientsTrainerActivity, "No tienes clientes nuevos pendientes.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@NewClientsTrainerActivity, "Error cargando nuevos clientes: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToClientDetail(client: Client) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                firestore.collection("users").document(client.uid).update("new", false).await()
                val intent = Intent(this@NewClientsTrainerActivity, ClientTrainerActivity::class.java)
                intent.putExtra("client_uid", client.uid)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this@NewClientsTrainerActivity, "Error al actualizar estado de cliente: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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

    override fun onResume() {
        super.onResume()
        val (email, role, uid) = sessionManager.getSession()
        if (uid != null && role == MainActivity.ROLE_TRAINER) {
            loadNewClients(uid)
        }
    }
}