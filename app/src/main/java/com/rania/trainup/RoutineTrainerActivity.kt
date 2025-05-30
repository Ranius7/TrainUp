package com.rania.trainup

import RoutineDayAdapter
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
        supportActionBar?.title = "RUTINA SEMANAL"
        binding.toolbarRutina.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        routineDayAdapter = RoutineDayAdapter(
            routineDaysList,
            true,
            onClick = { clickedRoutineDay ->
                navigateToTrainingTrainer(clickedRoutineDay)
            },
            onEditClick = { routineDayToEdit ->
                showEditRoutineDialog(routineDayToEdit.muscleGroup) { newName ->
                    val index = routineDaysList.indexOf(routineDayToEdit)
                    if (index != -1) {
                        val updatedDay = routineDayToEdit.copy(
                            muscleGroup = newName,
                            routineName = newName
                        )
                        routineDaysList[index] = updatedDay
                        routineDayAdapter.notifyItemChanged(index)
                        saveRoutine()
                    }
                }
            },
            onDeleteClick = { routineDayToDelete ->
                val position = routineDaysList.indexOf(routineDayToDelete)
                if (position != -1) showDeleteDayDialog(position)
            }
        )

        binding.rvRutinaSemanal.apply {
            layoutManager = LinearLayoutManager(this@RoutineTrainerActivity)
            adapter = routineDayAdapter
        }

        binding.bottomNavigationTrainer.selectedItemId = R.id.nav_clients

        loadClientRoutine(clientUid!!, trainerUid!!)
        setupBottomNavigationView()
        setupClickListeners()
        setupAddDayButton()

        binding.btnEliminar.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_confirm_delete, null)
            val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
            val tvMessage = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
            val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
            val btnDelete = dialogView.findViewById<Button>(R.id.btnDelete)

            tvTitle.text = "Eliminar rutina"
            tvMessage.text = "¿Seguro que quieres eliminar la rutina completa de este cliente?"

            val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            btnCancel.setOnClickListener { dialog.dismiss() }
            btnDelete.setOnClickListener {
                deleteRoutine()
                dialog.dismiss()
            }
            dialog.show()
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
            val dialogView = layoutInflater.inflate(R.layout.dialog_edit_routine, null)
            val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
            val etRoutineName = dialogView.findViewById<EditText>(R.id.etRoutineName)
            val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
            val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

            tvTitle.text = "Añadir nuevo día"
            etRoutineName.hint = "Ej: Lunes, Día 1, etc."

            val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            btnCancel.setOnClickListener { dialog.dismiss() }
            btnSave.setOnClickListener {
                val dayName = etRoutineName.text.toString().trim()
                if (dayName.isNotEmpty()) {
                    addRoutineDay(dayName)
                    dialog.dismiss()
                } else {
                    etRoutineName.error = "El nombre del día no puede estar vacío"
                }
            }
            dialog.show()
        }
    }

    private fun addRoutineDay(dayName: String) {
        val newDay = RoutineDay(
            routineName = dayName,
            muscleGroup = dayName,
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

    private fun showEditRoutineDialog(
        initialName: String = "",
        onSave: (String) -> Unit
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_routine, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etRoutineName = dialogView.findViewById<EditText>(R.id.etRoutineName)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        tvTitle.text = if (initialName.isEmpty()) "Añadir rutina" else "Editar rutina"
        etRoutineName.setText(initialName)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val name = etRoutineName.text.toString().trim()
            if (name.isNotEmpty()) {
                onSave(name)
                dialog.dismiss()
            } else {
                etRoutineName.error = "El nombre es obligatorio"
            }
        }
        dialog.show()
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

        val currentRoutine = WeeklyRoutine(
            clientUid = clientUid!!,
            trainerUid = trainerUid!!,
            routineDays = routineDaysList,
            published = true
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

    private fun showDeleteDayDialog(position: Int) {
        val day = routineDaysList.getOrNull(position) ?: return
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirm_delete, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnDelete = dialogView.findViewById<Button>(R.id.btnDelete)

        tvTitle.text = "Eliminar día"
        tvMessage.text = "¿Seguro que quieres eliminar el día \"${day.routineName}\"?"

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnDelete.setOnClickListener {
            routineDaysList.removeAt(position)
            routineDayAdapter.notifyDataSetChanged()
            saveRoutine()
            dialog.dismiss()
        }
        dialog.show()
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