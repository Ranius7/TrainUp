package com.rania.trainup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
    private var routineListener: ListenerRegistration? = null;

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

        // INICIALIZA EL ADAPTADOR Y EL RECYCLERVIEW AQUÍ:
        routineDayAdapter = RoutineDayAdapter(
            routineDaysList,
            false,
            { clickedRoutineDay -> navigateToTrainingClient(clickedRoutineDay) },
            null
        )
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

                    routineListener?.remove()
                    routineListener = firestore.collection("users").document(trainerUid)
                        .collection("routines").document(clientUid)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                Toast.makeText(this@RoutineClientActivity, "Error en tiempo real: ${error.message}", Toast.LENGTH_SHORT).show()
                                return@addSnapshotListener
                            }
                            if (snapshot != null && snapshot.exists()) {
                                val weeklyRoutine = snapshot.toObject(WeeklyRoutine::class.java)
                                if (weeklyRoutine != null && weeklyRoutine.published) {
                                    routineDaysList.clear()
                                    routineDaysList.addAll(weeklyRoutine.routineDays)
                                    routineDayAdapter.notifyDataSetChanged()
                                    Toast.makeText(this@RoutineClientActivity, "Días cargados: ${routineDaysList.size}", Toast.LENGTH_SHORT).show()
                                } else {
                                    routineDaysList.clear()
                                    routineDayAdapter.notifyDataSetChanged()
                                    Toast.makeText(this@RoutineClientActivity, "No hay rutina publicada actualmente.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                routineDaysList.clear()
                                routineDayAdapter.notifyDataSetChanged()
                                Toast.makeText(this@RoutineClientActivity, "No se encontró una rutina asignada.", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(this@RoutineClientActivity, "Entrenador no asignado o datos incompletos.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RoutineClientActivity, "Error cargando plan de entrenamiento: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

override fun onDestroy() {
    super.onDestroy()
    routineListener?.remove()
    }

    private fun navigateToTrainingClient(routineDay: RoutineDay) {
        val intent = Intent(this, TrainingClientActivity::class.java)
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
                    true // Ya estás aquí, no hagas nada
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