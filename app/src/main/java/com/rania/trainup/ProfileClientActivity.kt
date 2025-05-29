package com.rania.trainup

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rania.trainup.databinding.ActivityProfileClientBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileClientActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileClientBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sessionManager: SessionManager
    private lateinit var historyAdapter: TrainingHistoryAdapter
    private val trainingHistoryList = mutableListOf<TrainingHistory>()

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

        setSupportActionBar(binding.profileToolbar)
        binding.profileToolbar.title = "" // Para usar el TextView personalizado

        historyAdapter = TrainingHistoryAdapter(trainingHistoryList)
        val rvTrainingHistory = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvTrainingHistory)
        rvTrainingHistory?.apply {
            layoutManager = LinearLayoutManager(this@ProfileClientActivity)
            adapter = historyAdapter
        }

        loadClientProfile(uid)
        setupBottomNavigationView()
    }

    private fun loadClientProfile(uid: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val documentSnapshot = firestore.collection("users").document(uid).get().await()
                if (documentSnapshot.exists()) {
                    val client = documentSnapshot.toObject(Client::class.java)
                    binding.tvToolbarTitleClient.text = client?.name?.uppercase() ?: "CLIENTE"

                    loadTrainingHistory(uid)
                } else {
                    Toast.makeText(this@ProfileClientActivity, "Datos de perfil no encontrados.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProfileClientActivity, "Error al cargar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadTrainingHistory(uid: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val historySnapshot = firestore.collection("users").document(uid).collection("training_history")
                    .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING) // Ordenar por fecha
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

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.profile_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.itSettings -> { // Asumo itSettings es el ID del icono de ajustes en profile_menu.xml
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
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
                    // Cambia esto: NO abras TrainingClientActivity directamente
                    startActivity(Intent(this, RoutineClientActivity::class.java))
                    finish()
                    true
                }
                R.id.itNavProfile -> {
                    true // Ya estamos aquí
                }
                else -> false
            }
        }
    }
}