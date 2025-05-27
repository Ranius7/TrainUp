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
import com.rania.trainup.databinding.ActivityTrainingTrainerBinding // Asumo este binding
import com.rania.trainup.databinding.ItemExerciseTrainerBinding // Asumo este binding para el diálogo de edición
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
    private var trainerUid: String? = null // El UID del entrenador actual
    private var currentRoutineDay: RoutineDay? = null // El día de la rutina que se está editando

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

        setSupportActionBar(binding.toolbarEntreno) // Asumo toolbarEntreno
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarEntreno.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.tvTituloDia.text = getString(R.string.day_exercise_title, currentRoutineDay!!.dayOfWeek.uppercase(), currentRoutineDay!!.muscleGroup.uppercase())

        // El adaptador para el entrenador: isTrainer=true y se pasa el callback de edición
        exerciseAdapter = ExerciseAdapter(exercisesList, true) { clickedExercise ->
            showEditExerciseDialog(clickedExercise)
        }
        binding.rvEjercicios.apply { // Asumo rvEjercicios
            layoutManager = LinearLayoutManager(this@TrainingTrainerActivity)
            adapter = exerciseAdapter
        }

        exercisesList.clear()
        exercisesList.addAll(currentRoutineDay!!.exercises)
        exerciseAdapter.notifyDataSetChanged()

        setupClickListeners()
        setupBottomNavigationView()
    }

    private fun setupClickListeners() {
        binding.btnAddExercise.setOnClickListener { // Asumo btnAgregarEjercicio
            showAddExerciseDialog()
        }
        binding.btnGuardarDia.setOnClickListener { // Asumo btnGuardarEntreno
            saveRoutineChanges()
        }
    }

    private fun showAddExerciseDialog() {
        val dialogBinding = ItemExerciseTrainerBinding.inflate(layoutInflater)
        // Asegúrate de que etDescripcion tiene inputType="text" en el layout XML
        AlertDialog.Builder(this)
            .setTitle("Añadir Ejercicio")
            .setView(dialogBinding.root)
            .setPositiveButton("Añadir") { dialog, _ ->
                val name = dialogBinding.etNombreEjercicio.text.toString().trim()
                val material = dialogBinding.etMaterial.text.toString().trim()
                val series = dialogBinding.etSeries.text.toString().toIntOrNull() ?: 0
                val repetitions = dialogBinding.etRepeticiones.text.toString().toIntOrNull() ?: 0
                val rest = dialogBinding.etDescanso.text.toString().toIntOrNull() ?: 0
                val description = dialogBinding.etDescripcion.text.toString().trim() // <-- libre

                if (name.isNotEmpty() && series > 0 && repetitions > 0) {
                    val newExercise = Exercise(name, material, series, repetitions, rest, description)
                    exercisesList.add(newExercise)
                    exerciseAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Completa los campos obligatorios (nombre, series, reps)", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
            .show()
    }

    private fun showEditExerciseDialog(exercise: Exercise) {
        val dialogBinding = ItemExerciseTrainerBinding.inflate(layoutInflater)
        dialogBinding.etNombreEjercicio.setText(exercise.name)
        dialogBinding.etMaterial.setText(exercise.material)
        dialogBinding.etSeries.setText(exercise.series.toString())
        dialogBinding.etRepeticiones.setText(exercise.repetitions.toString())
        dialogBinding.etDescanso.setText(exercise.rest.toString())
        dialogBinding.etDescripcion.setText(exercise.description)

        AlertDialog.Builder(this)
            .setTitle("Editar Ejercicio")
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar") { dialog, _ ->
                val name = dialogBinding.etNombreEjercicio.text.toString().trim()
                val material = dialogBinding.etMaterial.text.toString().trim()
                val series = dialogBinding.etSeries.text.toString().toIntOrNull() ?: 0
                val repetitions = dialogBinding.etRepeticiones.text.toString().toIntOrNull() ?: 0
                val rest = dialogBinding.etDescanso.text.toString().toIntOrNull() ?: 0
                val description = dialogBinding.etDescripcion.text.toString().trim()

                if (name.isNotEmpty() && series > 0 && repetitions > 0) {
                    val updatedExercise = Exercise(name, material, series, repetitions, rest, description)
                    val index = exercisesList.indexOf(exercise)
                    if (index != -1) {
                        exercisesList[index] = updatedExercise
                        exerciseAdapter.notifyItemChanged(index)
                    }
                } else {
                    Toast.makeText(this, "Completa los campos obligatorios (nombre, series, reps)", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Eliminar") { dialog, _ ->
                exercisesList.remove(exercise)
                exerciseAdapter.notifyDataSetChanged()
                dialog.dismiss()
            }
            .setNeutralButton("Cancelar") { dialog, _ -> dialog.cancel() }
            .show()
    }

    private fun saveRoutineChanges() {
        val trainerUid = auth.currentUser?.uid ?: return
        if (clientUid == null || currentRoutineDay == null) return

        // Recalcular numExercises y numSets
        val numExercises = exercisesList.size
        val numSets = exercisesList.sumOf { it.series }

        val updatedRoutineDay = currentRoutineDay!!.copy(
            exercises = exercisesList,
            numExercises = numExercises,
            numSets = numSets
        )

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Obtener la rutina completa del entrenador para este cliente
                val routineDocRef = firestore.collection("users").document(trainerUid)
                    .collection("routines").document(clientUid!!)

                val routineSnapshot = routineDocRef.get().await()
                if (routineSnapshot.exists()) {
                    val weeklyRoutine = routineSnapshot.toObject(WeeklyRoutine::class.java)
                    weeklyRoutine?.let {
                        val updatedDays = it.routineDays.toMutableList()
                        val index = updatedDays.indexOfFirst { day -> day.dayOfWeek == updatedRoutineDay.dayOfWeek }
                        if (index != -1) {
                            updatedDays[index] = updatedRoutineDay
                        } else {
                            updatedDays.add(updatedRoutineDay) // Si no existía, la añade
                        }
                        val newWeeklyRoutine = it.copy(routineDays = updatedDays)
                        routineDocRef.set(newWeeklyRoutine).await()
                        Toast.makeText(this@TrainingTrainerActivity, "Cambios guardados. No olvides 'Publicar' la rutina.", Toast.LENGTH_LONG).show()
                        finish() // Volver a la rutina semanal del entrenador
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