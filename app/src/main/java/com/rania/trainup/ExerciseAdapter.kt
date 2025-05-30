package com.rania.trainup

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rania.trainup.databinding.ItemExerciseBinding

class ExerciseAdapter(
    private val exercises: MutableList<Exercise>,
    private val isTrainer: Boolean,
    private val onEditClick: ((Exercise) -> Unit)? = null
) : RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {

    class ExerciseViewHolder(val binding: ItemExerciseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val binding = ItemExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExerciseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exercise = exercises[position]
        holder.binding.tvExerciseNameClient.text = exercise.name
        holder.binding.tvExerciseDetailsClient.text = "${exercise.series} series · ${exercise.repetitions} reps · ${exercise.rest} descanso"
        holder.binding.tvMaterialClient.text = "Material: ${exercise.material.ifEmpty { "Ninguno" }}"
        holder.binding.tvDescriptionClient.text = "Descripción: ${exercise.description.ifEmpty { "N/A" }}"

        holder.binding.cbExerciseDone.visibility = if (isTrainer) android.view.View.GONE else android.view.View.VISIBLE

        holder.binding.cbExerciseDone.setOnCheckedChangeListener(null)
        holder.binding.cbExerciseDone.isChecked = exercise.isChecked
        holder.binding.cbExerciseDone.setOnCheckedChangeListener { _, isChecked ->
            exercise.isChecked = isChecked
        }

        if (isTrainer && onEditClick != null) {
            holder.binding.btnEditExercise.visibility = android.view.View.VISIBLE
            holder.binding.btnEditExercise.setOnClickListener {
                onEditClick.invoke(exercise)
            }
        } else {
            holder.binding.btnEditExercise.visibility = android.view.View.GONE
        }
    }

    override fun getItemCount(): Int = exercises.size

    fun updateExercises(newExercises: List<Exercise>) {
        exercises.clear()
        exercises.addAll(newExercises)
        notifyDataSetChanged()
    }

    fun uncheckAll() {
        exercises.forEach { it.isChecked = false }
        notifyDataSetChanged()
    }
}