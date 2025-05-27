package com.rania.trainup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rania.trainup.databinding.ActivityRoutineClientBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RoutineClientActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoutineClientBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sessionManager: SessionManager
    private lateinit var routineDayAdapter: RoutineDayAdapter
    private val routineDaysList = mutableListOf<RoutineDay>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoutineClientBinding.inflate(layoutInflater)
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


        // El adaptador no es para edición, por eso isTrainer=false y se pasa null como onEditClick
        routineDayAdapter = RoutineDayAdapter(routineDaysList, false, { clickedRoutineDay ->
            navigateToTrainingClient(clickedRoutineDay)
        }, null)
        binding.rvWeeklyRutineClient.apply {
            layoutManager = LinearLayoutManager(this@RoutineClientActivity)
            adapter = routineDayAdapter
        }

        loadClientTrainingPlan(uid)
        setupBottomNavigationView()
    }

    private fun loadClientTrainingPlan(clientUid: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val clientDoc = firestore.collection("users").document(clientUid).get().await()
                val trainerUid = clientDoc.getString("trainerUid")
                val clientName = clientDoc.getString("name")

                if (trainerUid != null) {
                    binding.tvTrainingPlanTitle.text = getString(R.string.training_plan_title, clientName ?: "tu")

                    val routinesSnapshot = firestore.collection("users").document(trainerUid)
                        .collection("routines").document(clientUid) // La rutina específica del cliente
                        .get().await()

                    if (routinesSnapshot.exists()) {
                        val weeklyRoutine = routinesSnapshot.toObject(WeeklyRoutine::class.java)
                        weeklyRoutine?.let {
                            routineDaysList.clear()
                            routineDaysList.addAll(it.routineDays)
                            routineDayAdapter.notifyDataSetChanged()
                        }
                    } else {
                        Toast.makeText(this@RoutineClientActivity, "No se encontró una rutina asignada.", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    Toast.makeText(this@RoutineClientActivity, "Entrenador no asignado o datos incompletos.", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@RoutineClientActivity, "Error cargando plan de entrenamiento: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToTrainingClient(routineDay: RoutineDay) {
        val intent = Intent(this, TrainingClientActivity::class.java) // Asumo TrainingClientActivity
        intent.putExtra("routine_day", routineDay) // Pasa el objeto RoutineDay
        startActivity(intent)
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

    override fun onResume() {
        super.onResume()
        val (email, role, uid) = sessionManager.getSession()
        if (uid != null && role == MainActivity.ROLE_CLIENT) {
            loadClientTrainingPlan(uid) // Recargar la rutina por si el entrenador la actualizó
        }
    }
}