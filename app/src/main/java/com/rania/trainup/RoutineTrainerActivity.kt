package com.rania.trainup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rania.trainup.databinding.ActivityRoutineTrainerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RoutineTrainerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoutineTrainerBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sessionManager: SessionManager
    private lateinit var routineDayAdapter: RoutineDayAdapter
    private val routineDaysList = mutableListOf<RoutineDay>()
    private var clientUid: String? = null
    private var trainerUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoutineTrainerBinding.inflate(layoutInflater)
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
        if (clientUid == null) {
            Toast.makeText(this, "UID de cliente no proporcionado.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setSupportActionBar(binding.toolbarRutina)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarRutina.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Adaptador para el entrenador: isTrainer=true y se pasa onEditClick
        routineDayAdapter = RoutineDayAdapter(
            routineDaysList,
            true,
            onItemClick = { clickedRoutineDay ->
                // Al hacer clic en un día, navegar a la edición de ejercicios de ese día
                navigateToTrainingTrainer(clickedRoutineDay)
            },
            onEditClick = { routineDayToEdit ->
                // Si clicó en el icono de editar específicamente
                navigateToTrainingTrainer(routineDayToEdit)
            },
            onDeleteClick = { position ->
                // Si clicó en el icono de eliminar
                showDeleteDayDialog(position)
            }
        )
        binding.rvRutinaSemanal.apply {
            layoutManager = LinearLayoutManager(this@RoutineTrainerActivity)
            adapter = routineDayAdapter
        }

        // Permitir eliminar día con pulsación larga, no funciona mirar esto otra vez
        binding.rvRutinaSemanal.addOnItemTouchListener(
            object : androidx.recyclerview.widget.RecyclerView.SimpleOnItemTouchListener() {
                fun onLongPress(e: android.view.MotionEvent) {
                    val child = binding.rvRutinaSemanal.findChildViewUnder(e.x, e.y)
                    if (child != null) {
                        val position = binding.rvRutinaSemanal.getChildAdapterPosition(child)
                        showDeleteDayDialog(position)
                    }
                }
            }
        )

        // No crear días automáticamente, solo cargar los existentes
        loadClientRoutine(clientUid!!, trainerUid!!)
        setupBottomNavigationView()
        setupClickListeners()
        setupAddDayButton() //botón para añadir día

        // Botón eliminar rutina COMPLETA
        binding.btnEliminar.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Eliminar rutina")
                .setMessage("¿Seguro que quieres eliminar la rutina completa de este cliente?")
                .setPositiveButton("Eliminar") { dialog, _ ->
                    deleteRoutine()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
                .show()
        }
    }

    private fun loadClientRoutine(clientUid: String, trainerUid: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val clientDoc = firestore.collection("users").document(clientUid).get().await()
                val clientName = clientDoc.getString("name") ?: "Cliente"
                binding.tvTituloRutina.text = getString(R.string.routine_title, clientName)

                val routineDocRef = firestore.collection("users").document(trainerUid)
                    .collection("routines").document(clientUid)

                val routineSnapshot = routineDocRef.get().await()

                if (routineSnapshot.exists()) {
                    val weeklyRoutine = routineSnapshot.toObject(WeeklyRoutine::class.java)
                    weeklyRoutine?.let {
                        routineDaysList.clear()
                        routineDaysList.addAll(it.routineDays)
                        routineDayAdapter.notifyDataSetChanged()
                    }
                } else {
                    // No crear días por defecto, dejar la lista vacía
                    routineDaysList.clear()
                    routineDayAdapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RoutineTrainerActivity, "Error al cargar la rutina: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupAddDayButton() {
        binding.btnAddDay.setOnClickListener {
            // Mostrar AlertDialog simple para añadir un nuevo día (nombre libre)
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Añadir nuevo día")
            val input = android.widget.EditText(this)
            input.hint = "Ej: Lunes, Día 1, etc."
            builder.setView(input)
            builder.setPositiveButton("Añadir") { dialog, _ ->
                val dayName = input.text.toString().trim()
                if (dayName.isNotEmpty()) {
                    addRoutineDay(dayName)
                } else {
                    android.widget.Toast.makeText(this, "El nombre del día no puede estar vacío", android.widget.Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
            builder.show()
        }
    }

    private fun addRoutineDay(dayName: String) {
        val newDay = RoutineDay(
            dayOfWeek = dayName,
            muscleGroup = "",
            exercises = emptyList(),
            numExercises = 0,
            numSets = 0
        )
        routineDaysList.add(newDay)
        routineDayAdapter.notifyDataSetChanged()
        saveRoutine()
    }

    private fun saveRoutine() {
        if (clientUid == null || trainerUid == null) return
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val routineDocRef = firestore.collection("users").document(trainerUid!!)
                    .collection("routines").document(clientUid!!)
                val snapshot = routineDocRef.get().await()
                val published = snapshot.getBoolean("published") ?: false
                val routine = WeeklyRoutine(
                    clientUid = clientUid!!,
                    trainerUid = trainerUid!!,
                    routineDays = routineDaysList,
                    published = published
                )
                routineDocRef.set(routine).await()
            } catch (_: Exception) {}
        }
    }

    private fun navigateToTrainingTrainer(routineDay: RoutineDay) {
        val intent = Intent(this, TrainingTrainerActivity::class.java)
        intent.putExtra("client_uid", clientUid)
        intent.putExtra("routine_day", routineDay)
        startActivity(intent)
    }

    private fun setupClickListeners() {
        binding.btnPublicarRutina.setOnClickListener {
            publishRoutine()
        }
    }

    private fun publishRoutine() {
        if (clientUid == null || trainerUid == null) {
            Toast.makeText(this, "Datos de cliente o entrenador no disponibles.", Toast.LENGTH_SHORT).show()
            return
        }

        // Actualizar la rutina en Firestore, marcándola como publicada
        val currentRoutine = WeeklyRoutine(
            clientUid = clientUid!!,
            trainerUid = trainerUid!!,
            routineDays = routineDaysList,
            published = true // Marcar como publicada
        )

        CoroutineScope(Dispatchers.Main).launch {
            try {
                firestore.collection("users").document(trainerUid!!)
                    .collection("routines").document(clientUid!!)
                    .set(currentRoutine)
                    .await()
                Toast.makeText(this@RoutineTrainerActivity, "Rutina publicada con éxito.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@RoutineTrainerActivity, "Error al publicar rutina: ${e.message}", Toast.LENGTH_SHORT).show()
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

    override fun onResume() {
        super.onResume()
        clientUid?.let { loadClientRoutine(it, trainerUid ?: "") }
    }

    // Permitir eliminar día con pulsación larga, no funciona bien tampoco, volver a mirarlo
    private fun showDeleteDayDialog(position: Int) {
        val day = routineDaysList.getOrNull(position) ?: return
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Eliminar día")
            .setMessage("¿Seguro que quieres eliminar el día \"${day.dayOfWeek}\"?")
            .setPositiveButton("Eliminar") { dialog, _ ->
                routineDaysList.removeAt(position)
                routineDayAdapter.notifyDataSetChanged()
                saveRoutine()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
            .show()
    }

    private fun deleteRoutine() {
        if (clientUid == null || trainerUid == null) return
        CoroutineScope(Dispatchers.Main).launch {
            try {
                firestore.collection("users").document(trainerUid!!)
                    .collection("routines").document(clientUid!!)
                    .delete().await()
                routineDaysList.clear()
                routineDayAdapter.notifyDataSetChanged()
                Toast.makeText(this@RoutineTrainerActivity, "Rutina eliminada.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@RoutineTrainerActivity, "Error al eliminar rutina: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}