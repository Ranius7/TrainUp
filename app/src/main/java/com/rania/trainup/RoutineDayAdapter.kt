package com.rania.trainup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.rania.trainup.databinding.ItemRoutineDayBinding
import com.rania.trainup.databinding.ItemRoutineDayTrainerBinding

class RoutineDayAdapter(
    private val items: List<RoutineDay>,
    private val isTrainer: Boolean,
    private val onItemClick: (RoutineDay) -> Unit,
    private val onEditClick: ((RoutineDay) -> Unit)? = null,
    private val onDeleteClick: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ClientViewHolder(val binding: ItemRoutineDayBinding) : RecyclerView.ViewHolder(binding.root)
    class TrainerViewHolder(val binding: ItemRoutineDayTrainerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int = if (isTrainer) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 1) {
            val binding = ItemRoutineDayTrainerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            TrainerViewHolder(binding)
        } else {
            val binding = ItemRoutineDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ClientViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val day = items[position]
        if (holder is TrainerViewHolder) {
            holder.binding.tvDay.text = "${day.dayOfWeek}: ${day.muscleGroup}"
            holder.binding.btnEditarDia.visibility = if (onEditClick != null) View.VISIBLE else View.GONE
            holder.binding.btnEditarDia.setOnClickListener { onEditClick?.invoke(day) }
            holder.binding.btnEliminarDia.setOnClickListener { onDeleteClick?.invoke(position) }
            holder.binding.root.setOnClickListener {
                Toast.makeText(holder.binding.root.context, "Click en ${day.dayOfWeek}", Toast.LENGTH_SHORT).show()
                onItemClick(day) }
        } else if (holder is ClientViewHolder) {
            holder.binding.tvDayClient.text = day.dayOfWeek
            holder.binding.tvRutineDayClient.text = day.muscleGroup
            holder.binding.tvDetailsRutineClient.text = "${day.numExercises} ejercicios - ${day.numSets} series"
            holder.binding.root.setOnClickListener { onItemClick(day) }
        }
    }

    override fun getItemCount(): Int = items.size
}