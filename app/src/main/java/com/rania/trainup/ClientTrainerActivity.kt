package com.rania.trainup

import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rania.trainup.com.rania.trainup.DailyTask
import com.rania.trainup.databinding.ActivityClientTrainerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

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

        binding.btnAddDailyTask.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun loadClientDetails(uid: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val clientDoc = firestore.collection("users").document(uid).get().await()
                if (clientDoc.exists()) {
                    val client = clientDoc.toObject(Client::class.java)
                    client?.let {
                        binding.tvClientName.text = it.name.uppercase()
                        binding.tvClientInfo.text = it.objective
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
            val intent = Intent(this, RoutineTrainerActivity::class.java)
            intent.putExtra("client_uid", clientUid)
            startActivity(intent)
        }
        binding.cardProgress.setOnClickListener {
            val intent = Intent(this, ProgressTrainerActivity::class.java)
            intent.putExtra("client_uid", clientUid)
            startActivity(intent)
        }
    }

    private fun showAddTaskDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.etTaskTitle)
        val etDesc = dialogView.findViewById<EditText>(R.id.etTaskDescription)

        AlertDialog.Builder(this)
            .setTitle("Nueva tarea diaria")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val title = etTitle.text.toString().trim()
                val desc = etDesc.text.toString().trim()
                if (title.isNotEmpty()) {
                    saveDailyTask(title, desc)
                } else {
                    Toast.makeText(this, "El título es obligatorio", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveDailyTask(title: String, desc: String) {
        val taskId = firestore.collection("users").document(clientUid!!).collection("daily_tasks").document().id
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
        val task = DailyTask(
            id = taskId,
            title = title,
            description = desc,
            date = today,
            isCompleted = false
        )
        CoroutineScope(Dispatchers.Main).launch {
            try {
                firestore.collection("users").document(clientUid!!)
                    .collection("daily_tasks").document(taskId)
                    .set(task).await()
                Toast.makeText(this@ClientTrainerActivity, "Tarea añadida", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@ClientTrainerActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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