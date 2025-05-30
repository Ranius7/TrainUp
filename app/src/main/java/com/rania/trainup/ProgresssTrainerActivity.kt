package com.rania.trainup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rania.trainup.databinding.ActivityProgressTrainerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProgressTrainerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProgressTrainerBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sessionManager: SessionManager
    private lateinit var historyAdapter: TrainingHistoryAdapter
    private val trainingHistoryList = mutableListOf<TrainingHistory>()
    private var clientUid: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgressTrainerBinding.inflate(layoutInflater)
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

        setSupportActionBar(binding.toolbarProgress)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarProgress.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        historyAdapter = TrainingHistoryAdapter(trainingHistoryList)
        val rvTrainingHistory = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvTrainingHistory)
        rvTrainingHistory?.apply {
            layoutManager = LinearLayoutManager(this@ProgressTrainerActivity)
            adapter = historyAdapter
        }

        loadClientTrainingHistory(clientUid!!)
        setupBottomNavigationView()
    }


    private fun loadClientTrainingHistory(clientUid: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val historySnapshot = firestore.collection("users").document(clientUid)
                    .collection("training_history")
                    .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get().await()

                trainingHistoryList.clear()
                for (doc in historySnapshot.documents) {
                    doc.toObject(TrainingHistory::class.java)?.let { trainingHistoryList.add(it) }
                }
                historyAdapter.notifyDataSetChanged()
                if (trainingHistoryList.isEmpty()) {
                    Toast.makeText(this@ProgressTrainerActivity, "Este cliente aún no tiene historial.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProgressTrainerActivity, "Error al cargar historial: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this@ProgressTrainerActivity, "Aún no hay historial de entrenamientos para este cliente.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProgressTrainerActivity, "Error al cargar historial: ${e.message}", Toast.LENGTH_SHORT).show()
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
}