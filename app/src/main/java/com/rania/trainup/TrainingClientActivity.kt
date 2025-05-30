package com.rania.trainup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rania.trainup.databinding.ActivityTrainingClientBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrainingClientActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrainingClientBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sessionManager: SessionManager
    private lateinit var exerciseAdapter: ExerciseAdapter
    private val exercisesList = mutableListOf<Exercise>()
    private var currentRoutineDay: RoutineDay? = null
    private var startTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrainingClientBinding.inflate(layoutInflater)
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

        setSupportActionBar(binding.toolbarClient)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "PLAN DE ENTRENAMIENTO"
        binding.toolbarClient.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        currentRoutineDay = intent.getParcelableExtra("routine_day")

        if (currentRoutineDay == null) {
            Toast.makeText(this, "Día de rutina no proporcionado.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.tvMuscleGroupTitle.text = currentRoutineDay?.muscleGroup?.uppercase() ?: ""
        binding.tvMaterialClient.text = getString(
            R.string.material_label,
            currentRoutineDay!!.exercises.joinToString(", ") { it.material }.ifEmpty { "Ninguno" }
        )

        exercisesList.clear()
        exercisesList.addAll(currentRoutineDay!!.exercises)
        exerciseAdapter = ExerciseAdapter(exercisesList, false)
        binding.rvExercisesClient.apply {
            layoutManager = LinearLayoutManager(this@TrainingClientActivity)
            adapter = exerciseAdapter
        }

        // Estado inicial de los botones
        binding.btnStartTraining.visibility = android.view.View.VISIBLE
        binding.btnStartTraining.isEnabled = true
        binding.btnFinishTraining.visibility = android.view.View.GONE
        binding.btnFinishTraining.isEnabled = true

        setupClickListeners()
        setupBottomNavigationView()
    }

    private fun setupClickListeners() {
        binding.btnStartTraining.setOnClickListener {
            startTime = System.currentTimeMillis()
            Toast.makeText(this, "Entrenamiento iniciado.", Toast.LENGTH_SHORT).show()
            binding.btnStartTraining.visibility = android.view.View.GONE
            binding.btnFinishTraining.visibility = android.view.View.VISIBLE
        }

        binding.btnFinishTraining.setOnClickListener {
            if (startTime == 0L) {
                Toast.makeText(this, "Debes iniciar el entrenamiento primero.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val endTime = System.currentTimeMillis()
            val durationMillis = endTime - startTime

            // Formatea la duración
            val hours = (durationMillis / (1000 * 60 * 60)).toInt()
            val minutes = ((durationMillis / (1000 * 60)) % 60).toInt()
            val seconds = ((durationMillis / 1000) % 60).toInt()
            val durationString = buildString {
                if (hours > 0) append("${hours}h ")
                if (minutes > 0 || hours > 0) append("${minutes} min ")
                append("${seconds} seg")
            }.trim()

            saveTrainingHistory(durationMillis, durationString)
            Toast.makeText(this, "Entrenamiento finalizado. Duración: $durationString.", Toast.LENGTH_LONG).show()

            // Desmarcar todos los ejercicios
            exerciseAdapter.uncheckAll()

            startTime = 0L
            binding.btnStartTraining.visibility = android.view.View.VISIBLE
            binding.btnFinishTraining.visibility = android.view.View.GONE
        }
    }

    private fun saveTrainingHistory(durationMillis: Long, durationString: String) {
        val uid = auth.currentUser?.uid ?: return
        val now = Date()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))
        val currentDate = dateFormat.format(now).uppercase(Locale("es", "ES"))

        val historyEntry = TrainingHistory(
            date = currentDate,
            trainingTitle = currentRoutineDay?.muscleGroup ?: "Entrenamiento",
            durationMinutes = (durationMillis / 1000 / 60).toInt().coerceAtLeast(1),
            durationFormatted = durationString,
            completed = true
        )

        firestore.collection("users").document(uid).collection("training_history")
            .add(historyEntry)
            .addOnSuccessListener {
                Toast.makeText(this, "Historial guardado.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar historial: ${e.message}", Toast.LENGTH_SHORT).show()
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
                R.id.itNavTraining -> true
                R.id.itNavProfile -> {
                    startActivity(Intent(this, ProfileClientActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}