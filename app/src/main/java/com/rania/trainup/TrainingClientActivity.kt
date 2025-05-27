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
        binding.toolbarClient.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }


        currentRoutineDay = intent.getParcelableExtra("routine_day")

        if (currentRoutineDay == null) {
            Toast.makeText(this, "Día de rutina no proporcionado.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.tvTrainingTitleClient.text = currentRoutineDay!!.muscleGroup
        binding.tvMaterialClient.text = getString(R.string.material_label, currentRoutineDay!!.exercises.joinToString(", ") { it.material }.ifEmpty { "Ninguno" })

        exerciseAdapter = ExerciseAdapter(exercisesList, false) // El cliente no edita, isTrainer=false
        binding.rvExercisesClient.apply {
            layoutManager = LinearLayoutManager(this@TrainingClientActivity)
            adapter = exerciseAdapter
        }

        exercisesList.clear()
        exercisesList.addAll(currentRoutineDay!!.exercises)
        exerciseAdapter.notifyDataSetChanged()

        // Estado inicial de los botones
        binding.btnStartTraining.isEnabled = true
        binding.btnFinishTraining.isEnabled = false
        binding.btnFinishTraining.visibility = android.view.View.INVISIBLE

        setupClickListeners()
        setupBottomNavigationView()
    }

    private fun setupClickListeners() {
        binding.btnStartTraining.setOnClickListener {
            startTime = System.currentTimeMillis()
            Toast.makeText(this, "Entrenamiento iniciado.", Toast.LENGTH_SHORT).show()
            binding.btnStartTraining.isEnabled = false
            binding.btnFinishTraining.isEnabled = true
            binding.btnFinishTraining.visibility = android.view.View.VISIBLE
        }

        binding.btnFinishTraining.setOnClickListener {
            if (startTime == 0L) {
                Toast.makeText(this, "Debes iniciar el entrenamiento primero.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val endTime = System.currentTimeMillis()
            val durationMillis = endTime - startTime
            val durationMinutes = (durationMillis / 1000 / 60).toInt().coerceAtLeast(1)

            saveTrainingHistory(durationMinutes)
            Toast.makeText(this, "Entrenamiento finalizado. Duración: $durationMinutes minutos.", Toast.LENGTH_LONG).show()

            startTime = 0L
            binding.btnStartTraining.isEnabled = true
            binding.btnFinishTraining.isEnabled = false
            binding.btnFinishTraining.visibility = android.view.View.INVISIBLE
        }

    }

    private fun saveTrainingHistory(durationMinutes: Int) {
        val uid = auth.currentUser?.uid ?: return
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES")) // Formato completo de fecha
        val currentDate = dateFormat.format(Date())

        val historyEntry = TrainingHistory(
            date = currentDate.uppercase(Locale.getDefault()),
            trainingTitle = currentRoutineDay?.muscleGroup ?: "Entrenamiento",
            durationMinutes = durationMinutes,
            completed = true
        )

        firestore.collection("users").document(uid).collection("training_history")
            .add(historyEntry) // add() genera un ID de documento automático
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
                R.id.itNavTraining -> {
                    true // Ya estamos aquí
                }
                R.id.itNavProfile -> {
                    startActivity(Intent(this, ProfileClientActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}