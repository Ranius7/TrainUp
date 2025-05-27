package com.rania.trainup

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rania.trainup.databinding.ItemDailyGoalsBinding

class GoalAdapter(
    private val goals: MutableList<Goal>,
    private val onGoalClick: (Goal) -> Unit // Callback para cuando se haga clic en un objetivo
) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    class GoalViewHolder(val binding: ItemDailyGoalsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val binding = ItemDailyGoalsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GoalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = goals[position]
        holder.binding.tvGoal.text = goal.text
        holder.binding.cbGoal.isChecked = goal.isCompleted
        holder.binding.cbGoal.setOnCheckedChangeListener { _, isChecked ->
            // No actualizamos el modelo aquí directamente, sino a través del callback
            // La idea es que la Activity/Fragment lo actualice en Firestore y recargue
            onGoalClick(goal.copy(isCompleted = isChecked))
        }
        holder.binding.root.setOnClickListener {
            // Si el clic en la tarjeta también alterna el estado
            onGoalClick(goal.copy(isCompleted = !goal.isCompleted))
        }
    }

    override fun getItemCount(): Int = goals.size

    fun updateGoals(newGoals: List<Goal>) {
        goals.clear()
        goals.addAll(newGoals)
        notifyDataSetChanged()
    }
}