package com.rania.trainup

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rania.trainup.databinding.ItemTrainingHistoryBinding

class TrainingHistoryAdapter(
    private val historyList: MutableList<TrainingHistory>
) : RecyclerView.Adapter<TrainingHistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(val binding: ItemTrainingHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemTrainingHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val history = historyList[position]
        holder.binding.tvDateHistoryClient.text = history.date
        holder.binding.tvTrainingTitleClient.text = "🏋️‍♀️ ${history.trainingTitle} | ${if (history.completed) "ENTRENAMIENTO COMPLETADO" else "PENDIENTE"}"
        holder.binding.tvTrainingDuration.text = "⏱️ Duración: ${history.durationMinutes} minutos"
    }

    override fun getItemCount(): Int = historyList.size

    fun updateHistory(newHistory: List<TrainingHistory>) {
        historyList.clear()
        historyList.addAll(newHistory)
        notifyDataSetChanged()
    }
}