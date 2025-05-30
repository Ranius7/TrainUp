package com.rania.trainup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rania.trainup.databinding.ActivityClientTrainerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import com.rania.trainup.com.rania.trainup.DailyTask

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
        supportActionBar?.title = "DETALLES DEL CLIENTE"
        binding.toolbarClientDetail.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.bottomNavigationTrainer.selectedItemId = R.id.nav_clients

        loadClientDetails(clientUid!!)
        setupClickListeners()
        setupBottomNavigationView()

        binding.cardDailyTasks.setOnClickListener {
            showDailyTasksDialog()
        }
    }

    private fun loadClientDetails(uid: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val clientDoc = firestore.collection("users").document(uid).get().await()
                if (clientDoc.exists()) {
                    val client = clientDoc.toObject(Client::class.java)
                    client?.let {
                        val nombreCliente = it.name.uppercase()
                        binding.tvClientName.text = nombreCliente
                        binding.tvClientInfo.text = it.objective


                        binding.tvCardRoutineTitle.text = "GESTIONAR RUTINAS DE $nombreCliente"
                        binding.tvCardProgressTitle.text = "VER HISTORIAL DE $nombreCliente"
                        binding.tvCardDailyTasksTitle.text = "TAREAS DE $nombreCliente"

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

    private fun showAddTaskDialog(onTaskAdded: (DailyTask) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etTaskTitle = dialogView.findViewById<EditText>(R.id.etTaskTitle)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        tvTitle.text = "Nueva tarea"
        etTaskTitle.hint = "Título de la tarea"

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val title = etTaskTitle.text.toString().trim()
            if (title.isNotEmpty()) {
                saveDailyTask(title, onTaskAdded)
                dialog.dismiss()
            } else {
                etTaskTitle.error = "El título es obligatorio"
            }
        }
        dialog.show()
    }

    private fun saveDailyTask(title: String, onTaskAdded: (DailyTask) -> Unit) {
        val taskId = firestore.collection("users").document(clientUid!!).collection("daily_tasks").document().id
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
        val task = DailyTask(
            id = taskId,
            title = title,
            description = "",
            date = today,
            isCompleted = false
        )
        CoroutineScope(Dispatchers.Main).launch {
            try {
                firestore.collection("users").document(clientUid!!)
                    .collection("daily_tasks").document(taskId)
                    .set(task).await()
                onTaskAdded(task)
                Toast.makeText(this@ClientTrainerActivity, "Tarea añadida", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@ClientTrainerActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDailyTasksDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_daily_tasks, null)
        val rvDailyTasks = dialogView.findViewById<RecyclerView>(R.id.rvDailyTasks)
        val btnAddTask = dialogView.findViewById<Button>(R.id.btnAddTask)
        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)

        val tasksList = mutableListOf<DailyTask>()
        lateinit var adapter: DailyTaskAdapter

        adapter = DailyTaskAdapter(
            tasksList,
            onCheckedChange = { _, _ -> },
            onEdit = { task -> showEditTaskDialog(task) },
            onDelete = { task -> deleteDailyTask(task, tasksList, adapter) }
        )

        rvDailyTasks.layoutManager = LinearLayoutManager(this)
        rvDailyTasks.adapter = adapter


        CoroutineScope(Dispatchers.Main).launch {
            val snapshot = firestore.collection("users").document(clientUid!!)
                .collection("daily_tasks").get().await()
            tasksList.clear()
            for (doc in snapshot.documents) {
                doc.toObject(DailyTask::class.java)?.let { tasksList.add(it) }
            }
            adapter.notifyDataSetChanged()
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnAddTask.setOnClickListener {
            showAddTaskDialog { newTask ->
                tasksList.add(newTask)
                adapter.notifyItemInserted(tasksList.size - 1)
            }
        }
        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showEditTaskDialog(task: DailyTask) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_input_field, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etInput = dialogView.findViewById<EditText>(R.id.etDialogInput)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        tvTitle.text = "Editar tarea"
        etInput.setText(task.title)
        etInput.hint = "Título de la tarea"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val newTitle = etInput.text.toString().trim()
            if (newTitle.isNotEmpty()) {
                val updatedTask = task.copy(title = newTitle)
                CoroutineScope(Dispatchers.Main).launch {
                    firestore.collection("users").document(clientUid!!)
                        .collection("daily_tasks").document(task.id)
                        .set(updatedTask).await()
                    Toast.makeText(this@ClientTrainerActivity, "Tarea actualizada", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            } else {
                etInput.error = "El título es obligatorio"
            }
        }
        dialog.show()
    }

    private fun deleteDailyTask(task: DailyTask, tasksList: MutableList<DailyTask>, adapter: DailyTaskAdapter) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                firestore.collection("users").document(clientUid!!)
                    .collection("daily_tasks").document(task.id)
                    .delete().await()
                val index = tasksList.indexOfFirst { it.id == task.id }
                if (index != -1) {
                    tasksList.removeAt(index)
                    adapter.notifyItemRemoved(index)
                }
                Toast.makeText(this@ClientTrainerActivity, "Tarea eliminada", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@ClientTrainerActivity, "Error al eliminar tarea: ${e.message}", Toast.LENGTH_SHORT).show()
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