package com.rania.trainup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.rania.trainup.databinding.ItemRoutineDayTrainerBinding // Usamos este layout para ambos

class RoutineDayAdapter(
    private val items: List<RoutineDay>,
    private val isTrainer: Boolean, // Para saber si es el entrenador el que ve la lista
    private val onItemClick: (RoutineDay) -> Unit, // Para ver detalles del día o editar
    private val onEditClick: ((RoutineDay) -> Unit)? = null, // Solo para el entrenador
    private val onDeleteClick: ((Int) -> Unit)? = null // Nuevo parámetro
) : RecyclerView.Adapter<RoutineDayAdapter.RoutineDayViewHolder>() {

    class RoutineDayViewHolder(val binding: ItemRoutineDayTrainerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoutineDayViewHolder {
        val binding = ItemRoutineDayTrainerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoutineDayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoutineDayViewHolder, position: Int) {
        val day = items[position]
        holder.binding.tvDay.text = "${day.dayOfWeek}: ${day.muscleGroup}"
        // Puedes añadir más detalles si los necesitas en el layout

        if (isTrainer && onEditClick != null) {
            holder.binding.btnEditarDia.visibility = View.VISIBLE
            holder.binding.btnEditarDia.setOnClickListener {
                onEditClick.invoke(day)
            }
        } else {
            holder.binding.btnEditarDia.visibility = View.GONE
        }

        holder.binding.root.setOnClickListener {
            onItemClick(day)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateRoutineDays(newRoutineDays: List<RoutineDay>) {
        (items as MutableList).clear()
        (items as MutableList).addAll(newRoutineDays)
        notifyDataSetChanged()
    }
}