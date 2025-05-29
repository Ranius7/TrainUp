package com.rania.trainup

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    private lateinit var goalAdapter: GoalAdapter
    private val goalsList = mutableListOf<Goal>()

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

        // Configurar RecyclerView para objetivos
        goalAdapter = GoalAdapter(goalsList) { clickedGoal ->
            toggleGoalCompletion(clickedGoal)
        }
        binding.rvObjetivos.apply { // Asumo rvDailyGoals es el ID de tu RecyclerView
            layoutManager = LinearLayoutManager(this@HomeClientActivity)
            adapter = goalAdapter
        }

        loadClientData(uid)
        setupBottomNavigationView()
        setupClickListeners()
    }

    private fun loadClientData(uid: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val documentSnapshot = firestore.collection("users").document(uid).get().await()
                if (documentSnapshot.exists()) {
                    val client = documentSnapshot.toObject(Client::class.java)
                    binding.tvWelcome.text = "👋 ¡Hola, ${client?.name ?: "Cliente"}!" // Asumo tvWelcome
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

    private fun toggleGoalCompletion(goal: Goal) {
        val uid = auth.currentUser?.uid ?: return
        val updatedGoal = goal.copy(isCompleted = !goal.isCompleted)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                firestore.collection("users").document(uid).collection("goals").document(goal.id).set(updatedGoal).await()
                loadGoals(uid) // Recargar objetivos para actualizar la vista
            } catch (e: Exception) {
                Toast.makeText(this@HomeClientActivity, "Error al actualizar objetivo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBottomNavigationView() {
        binding.bottomNavigationClient.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.itNavHome -> {
                    true // Ya estamos en Home
                }
                R.id.itNavTraining -> {
                    // NO HAGAS NADA aquí, o simplemente navega a RoutineClientActivity
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
        binding.btnAgregarObjetivo.setOnClickListener { // Asumo btnDailyGoals para agregar objetivo
            showAddGoalDialog()
        }
        binding.tvTaskTraining.setOnClickListener {
             // NO HAGAS NADA aquí, o simplemente navega a RoutineClientActivity
            startActivity(Intent(this, RoutineClientActivity::class.java))
        }
        binding.tvTaskProgress.setOnClickListener {
            // Mensaje simplificado para progreso
            Toast.makeText(this, "Funcionalidad de registro de progreso de peso simplificada.", Toast.LENGTH_SHORT).show()
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