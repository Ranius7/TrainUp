package com.rania.trainup

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rania.trainup.databinding.ItemTrainingHistoryBinding

class TrainingHistoryAdapter(
    private val historyList: List<TrainingHistory>
) : RecyclerView.Adapter<TrainingHistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(val binding: ItemTrainingHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemTrainingHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val history = historyList[position]
        holder.binding.tvDateHistoryClient.text = "📆 ${history.date}"
        holder.binding.tvMuscleGroupTitle.text = "🏋️‍♀️ ${history.trainingTitle} | ENTRENAMIENTO COMPLETADO"
        holder.binding.tvTrainingDuration.text = "⏱️ Duración: ${history.durationFormatted}"
    }

    override fun getItemCount(): Int = historyList.size
}