package com.rania.trainup

import GoalAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rania.trainup.com.rania.trainup.DailyTask
import com.rania.trainup.databinding.ActivityHomeClientBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeClientActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeClientBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sessionManager: SessionManager

    private val goalsList = mutableListOf<Goal>()
    private lateinit var goalAdapter: GoalAdapter

    private val tareasEntrenadorList = mutableListOf<DailyTask>()
    private lateinit var tareasEntrenadorAdapter: DailyTaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeClientBinding.inflate(layoutInflater)
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

        // Objetivos diarios
        goalAdapter = GoalAdapter(goalsList,
            onGoalCheckedChange = { goal, isChecked ->
                toggleGoalCompletion(goal, isChecked)
            },
            onGoalDelete = { goal ->
                val uid = auth.currentUser?.uid ?: return@GoalAdapter
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        firestore.collection("users").document(uid).collection("goals").document(goal.id).delete().await()
                        Toast.makeText(this@HomeClientActivity, "Objetivo eliminado", Toast.LENGTH_SHORT).show()
                        loadGoals(uid)
                    } catch (e: Exception) {
                        Toast.makeText(this@HomeClientActivity, "Error al eliminar objetivo: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        binding.rvObjetivos.adapter = goalAdapter
        binding.rvObjetivos.layoutManager = LinearLayoutManager(this)

        // Tareas del entrenador
        tareasEntrenadorAdapter = DailyTaskAdapter(tareasEntrenadorList) { task, isChecked ->
            toggleTaskCompletion(task, isChecked)
        }
        binding.rvTareasDiarias.adapter = tareasEntrenadorAdapter
        binding.rvTareasDiarias.layoutManager = LinearLayoutManager(this)

        loadClientData(uid)
        setupBottomNavigationView()
        setupClickListeners()
        loadTareasEntrenador(uid)
    }

    private fun loadClientData(uid: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val documentSnapshot = firestore.collection("users").document(uid).get().await()
                if (documentSnapshot.exists()) {
                    val client = documentSnapshot.toObject(Client::class.java)
                    binding.tvWelcome.text = "👋 ¡Hola, ${client?.name ?: "Cliente"}!"
                    binding.tvDate.text = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("es", "ES")).format(Date()).replaceFirstChar { it.titlecase(Locale("es", "ES")) }
                    loadGoals(uid)
                } else {
                    Toast.makeText(this@HomeClientActivity, "Datos de cliente no encontrados.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@HomeClientActivity, "Error al cargar datos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadTareasEntrenador(uid: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val tasksSnapshot = firestore.collection("users").document(uid)
                    .collection("daily_tasks")
                    .whereEqualTo("date", today)
                    .get().await()
                tareasEntrenadorList.clear()
                for (doc in tasksSnapshot.documents) {
                    doc.toObject(DailyTask::class.java)?.let { tareasEntrenadorList.add(it) }
                }
                tareasEntrenadorAdapter.notifyDataSetChanged()
                // Mostrar u ocultar el mensaje según haya tareas o no
                binding.tvNoTareasEntrenador.visibility =
                    if (tareasEntrenadorList.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(this@HomeClientActivity, "Error al cargar tareas: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.tvNoTareasEntrenador.visibility = View.VISIBLE
            }
        }
    }

    private fun toggleTaskCompletion(task: DailyTask, isChecked: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        if (isChecked) {
            // Si se marca como hecha, elimina la tarea de Firestore y de la lista local, no se si lo quiero así, de momento lo dejo
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    firestore.collection("users").document(uid)
                        .collection("daily_tasks").document(task.id)
                        .delete().await()
                    val index = tareasEntrenadorList.indexOfFirst { it.id == task.id }
                    if (index != -1) {
                        tareasEntrenadorList.removeAt(index)
                        tareasEntrenadorAdapter.notifyItemRemoved(index)
                    }
                    // Mostrar mensaje si ya no quedan tareas
                    binding.tvNoTareasEntrenador.visibility =
                        if (tareasEntrenadorList.isEmpty()) View.VISIBLE else View.GONE
                } catch (e: Exception) {
                    Toast.makeText(this@HomeClientActivity, "Error al eliminar tarea: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Si se desmarca, simplemente se actualiza el estado
            val updatedTask = task.copy(isCompleted = false)
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    firestore.collection("users").document(uid)
                        .collection("daily_tasks").document(task.id)
                        .set(updatedTask).await()
                    val index = tareasEntrenadorList.indexOfFirst { it.id == task.id }
                    if (index != -1) {
                        tareasEntrenadorList[index] = updatedTask
                        tareasEntrenadorAdapter.notifyItemChanged(index)
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@HomeClientActivity, "Error al actualizar tarea: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadGoals(uid: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val goalsSnapshot = firestore.collection("users").document(uid).collection("goals").get().await()
                goalsList.clear()
                for (doc in goalsSnapshot.documents) {
                    doc.toObject(Goal::class.java)?.let { goalsList.add(it) }
                }
                goalAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Toast.makeText(this@HomeClientActivity, "Error al cargar objetivos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleGoalCompletion(goal: Goal, isChecked: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val updatedGoal = goal.copy(isCompleted = isChecked)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                firestore.collection("users").document(uid).collection("goals").document(goal.id).set(updatedGoal).await()
                val index = goalsList.indexOfFirst { it.id == goal.id }
                if (index != -1) {
                    goalsList[index] = updatedGoal
                    goalAdapter.notifyItemChanged(index)
                }
            } catch (e: Exception) {
                Toast.makeText(this@HomeClientActivity, "Error al actualizar objetivo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBottomNavigationView() {
        binding.bottomNavigationClient.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.itNavHome -> true
                R.id.itNavTraining -> {
                    startActivity(Intent(this, RoutineClientActivity::class.java))
                    true
                }
                R.id.itNavProfile -> {
                    startActivity(Intent(this, ProfileClientActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnAgregarObjetivo.setOnClickListener {
            showAddGoalDialog()
        }
        
    }

    private fun showAddGoalDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Añadir Nuevo Objetivo")
        val input = EditText(this)
        input.hint = "Ej: Correr 5km"
        builder.setView(input)

        builder.setPositiveButton("Añadir") { dialog, _ ->
            val goalText = input.text.toString().trim()
            if (goalText.isNotEmpty()) {
                addGoal(goalText)
            } else {
                Toast.makeText(this, "El objetivo no puede estar vacío", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    private fun addGoal(goalText: String) {
        val uid = auth.currentUser?.uid ?: return
        val newGoalRef = firestore.collection("users").document(uid).collection("goals").document()
        val newGoal = Goal(newGoalRef.id, goalText, false)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                newGoalRef.set(newGoal).await()
                Toast.makeText(this@HomeClientActivity, "Objetivo añadido", Toast.LENGTH_SHORT).show()
                loadGoals(uid)
            } catch (e: Exception) {
                Toast.makeText(this@HomeClientActivity, "Error al añadir objetivo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}