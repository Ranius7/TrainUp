package com.rania.trainup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rania.trainup.databinding.ActivityTrainingTrainerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TrainingTrainerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrainingTrainerBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sessionManager: SessionManager
    private lateinit var exerciseAdapter: ExerciseAdapter
    private val exercisesList = mutableListOf<Exercise>()
    private var clientUid: String? = null
    private var trainerUid: String? = null
    private var currentRoutineDay: RoutineDay? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrainingTrainerBinding.inflate(layoutInflater)
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
        trainerUid = uid

        clientUid = intent.getStringExtra("client_uid")
        currentRoutineDay = intent.getParcelableExtra("routine_day")

        if (clientUid == null || currentRoutineDay == null) {
            Toast.makeText(this, "Datos incompletos para editar ejercicios.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        setSupportActionBar(binding.toolbarEntreno)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "EJERCICIOS"
        binding.toolbarEntreno.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.tvTituloDia.text = currentRoutineDay!!.routineName.uppercase()

        exerciseAdapter = ExerciseAdapter(exercisesList, true) { clickedExercise ->
            showEditExerciseDialog(clickedExercise)
        }
        binding.rvEjercicios.apply {
            layoutManager = LinearLayoutManager(this@TrainingTrainerActivity)
            adapter = exerciseAdapter
        }

        exercisesList.clear()
        exercisesList.addAll(currentRoutineDay!!.exercises)
        exerciseAdapter.notifyDataSetChanged()

        binding.bottomNavigationTrainer.selectedItemId = R.id.nav_clients

        setupClickListeners()
        setupBottomNavigationView()
    }

    private fun setupClickListeners() {
        binding.btnAddExercise.setOnClickListener {
            showAddExerciseDialog()
        }
        binding.btnGuardarDia.setOnClickListener {
            saveRoutineChanges()
        }
    }

    private fun showAddExerciseDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_exercise, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombreEjercicio)
        val etMaterial = dialogView.findViewById<EditText>(R.id.etMaterial)
        val etSeries = dialogView.findViewById<EditText>(R.id.etSeries)
        val etReps = dialogView.findViewById<EditText>(R.id.etRepeticiones)
        val etDescanso = dialogView.findViewById<EditText>(R.id.etDescanso)
        val etDescripcion = dialogView.findViewById<EditText>(R.id.etDescripcion)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        tvTitle.text = "Añadir ejercicio"

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val name = etNombre.text.toString().trim()
            val material = etMaterial.text.toString().trim()
            val series = etSeries.text.toString().toIntOrNull() ?: 0
            val repetitions = etReps.text.toString().toIntOrNull() ?: 0
            val rest = etDescanso.text.toString().trim()
            val description = etDescripcion.text.toString().trim()

            if (name.isNotEmpty() && series > 0 && repetitions > 0) {
                val newExercise = Exercise(name, material, series, repetitions, rest, description)
                exercisesList.add(newExercise)
                exerciseAdapter.notifyDataSetChanged()
                dialog.dismiss()
            } else {
                if (name.isEmpty()) etNombre.error = "Obligatorio"
                if (series <= 0) etSeries.error = "Obligatorio"
                if (repetitions <= 0) etReps.error = "Obligatorio"
            }
        }
        dialog.show()
    }

    private fun showEditExerciseDialog(exercise: Exercise) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_exercise, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombreEjercicio)
        val etMaterial = dialogView.findViewById<EditText>(R.id.etMaterial)
        val etSeries = dialogView.findViewById<EditText>(R.id.etSeries)
        val etReps = dialogView.findViewById<EditText>(R.id.etRepeticiones)
        val etDescanso = dialogView.findViewById<EditText>(R.id.etDescanso)
        val etDescripcion = dialogView.findViewById<EditText>(R.id.etDescripcion)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        tvTitle.text = "Editar ejercicio"
        etNombre.setText(exercise.name)
        etMaterial.setText(exercise.material)
        etSeries.setText(exercise.series.toString())
        etReps.setText(exercise.repetitions.toString())
        etDescanso.setText(exercise.rest)
        etDescripcion.setText(exercise.description)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val name = etNombre.text.toString().trim()
            val material = etMaterial.text.toString().trim()
            val series = etSeries.text.toString().toIntOrNull() ?: 0
            val repetitions = etReps.text.toString().toIntOrNull() ?: 0
            val rest = etDescanso.text.toString().trim()
            val description = etDescripcion.text.toString().trim()

            if (name.isNotEmpty() && series > 0 && repetitions > 0) {
                val updatedExercise = Exercise(name, material, series, repetitions, rest, description)
                val index = exercisesList.indexOf(exercise)
                if (index != -1) {
                    exercisesList[index] = updatedExercise
                    exerciseAdapter.notifyItemChanged(index)
                }
                dialog.dismiss()
            } else {
                if (name.isEmpty()) etNombre.error = "Obligatorio"
                if (series <= 0) etSeries.error = "Obligatorio"
                if (repetitions <= 0) etReps.error = "Obligatorio"
            }
        }
        dialog.show()
    }

    private fun saveRoutineChanges() {
        val trainerUid = auth.currentUser?.uid ?: return
        if (clientUid == null || currentRoutineDay == null) return

        val numExercises = exercisesList.size
        val numSets = exercisesList.sumOf { it.series }

        val updatedRoutineDay = currentRoutineDay!!.copy(
            exercises = exercisesList,
            numExercises = numExercises,
            numSets = numSets
        )

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val routineDocRef = firestore.collection("users").document(trainerUid)
                    .collection("routines").document(clientUid!!)

                val routineSnapshot = routineDocRef.get().await()
                if (routineSnapshot.exists()) {
                    val weeklyRoutine = routineSnapshot.toObject(WeeklyRoutine::class.java)
                    weeklyRoutine?.let {
                        val updatedDays = it.routineDays.toMutableList()
                        val index = updatedDays.indexOfFirst { day -> day.routineName == updatedRoutineDay.routineName }
                        if (index != -1) {
                            updatedDays[index] = updatedRoutineDay
                        } else {
                            updatedDays.add(updatedRoutineDay)
                        }
                        val newWeeklyRoutine = it.copy(routineDays = updatedDays)
                        routineDocRef.set(newWeeklyRoutine).await()
                        Toast.makeText(this@TrainingTrainerActivity, "Cambios guardados. No olvides 'Publicar' la rutina.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this@TrainingTrainerActivity, "No se encontró la rutina para actualizar. Crea una nueva.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TrainingTrainerActivity, "Error al guardar cambios: ${e.message}", Toast.LENGTH_SHORT).show()
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