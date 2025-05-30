package com.rania.trainup

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rania.trainup.databinding.ActivityProfileClientBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileClientActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileClientBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sessionManager: SessionManager
    private lateinit var historyAdapter: TrainingHistoryAdapter
    private val trainingHistoryList = mutableListOf<TrainingHistory>()
    private var clientName: String = "CLIENTE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileClientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        sessionManager = SessionManager(this)

        val (email, role, uid) = sessionManager.getSession()

        if (uid == null || role != MainActivity.ROLE_CLIENT) {
            Toast.makeText(this, "Sesión no válida. Redirigiendo al inicio.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setSupportActionBar(binding.toolbarProfile)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarProfile.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // El menú de ajustes se gestiona con onCreateOptionsMenu/onOptionsItemSelected

        // Accede al RecyclerView incluido usando findViewById sobre la raíz del binding
        val rvTrainingHistory = binding.root.findViewById<RecyclerView>(R.id.rvTrainingHistory)
        historyAdapter = TrainingHistoryAdapter(trainingHistoryList)
        rvTrainingHistory.layoutManager = LinearLayoutManager(this)
        rvTrainingHistory.adapter = historyAdapter

        binding.bottomNavigationClient.selectedItemId = R.id.itNavProfile

        loadClientProfile(uid)
        setupBottomNavigationView()
    }

    private fun loadClientProfile(uid: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val documentSnapshot = firestore.collection("users").document(uid).get().await()
                if (documentSnapshot.exists()) {
                    val client = documentSnapshot.toObject(Client::class.java)
                    clientName = client?.name?.uppercase() ?: "CLIENTE"
                    supportActionBar?.title = clientName
                    loadTrainingHistory(uid)
                } else {
                    supportActionBar?.title = "CLIENTE"
                    Toast.makeText(this@ProfileClientActivity, "Datos de perfil no encontrados.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                supportActionBar?.title = "CLIENTE"
                Toast.makeText(this@ProfileClientActivity, "Error al cargar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadTrainingHistory(uid: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val historySnapshot = firestore.collection("users").document(uid).collection("training_history")
                    .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get().await()

                trainingHistoryList.clear()
                for (doc in historySnapshot.documents) {
                    doc.toObject(TrainingHistory::class.java)?.let { trainingHistoryList.add(it) }
                }
                historyAdapter.notifyDataSetChanged()
                if (trainingHistoryList.isEmpty()) {
                    Toast.makeText(this@ProfileClientActivity, "Aún no tienes historial de entrenamientos.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProfileClientActivity, "Error al cargar historial: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.profile_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.itSettings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupBottomNavigationView() {
        binding.bottomNavigationClient.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.itNavHome -> {
                    startActivity(Intent(this, HomeClientActivity::class.java))
                    finish()
                    true
                }
                R.id.itNavTraining -> {
                    startActivity(Intent(this, RoutineClientActivity::class.java))
                    finish()
                    true
                }
                R.id.itNavProfile -> {
                    true
                }
                else -> false
            }
        }
    }
}