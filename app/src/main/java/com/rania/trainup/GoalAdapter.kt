import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rania.trainup.Goal
import com.rania.trainup.databinding.ItemDailyGoalsBinding

class GoalAdapter(
    private val goals: MutableList<Goal>,
    private val onGoalCheckedChange: (Goal, Boolean) -> Unit,
    private val onGoalDelete: (Goal) -> Unit // Nuevo callback para eliminar
) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    class GoalViewHolder(val binding: ItemDailyGoalsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val binding = ItemDailyGoalsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GoalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = goals[position]
        holder.binding.tvGoal.text = goal.text
        holder.binding.cbGoal.setOnCheckedChangeListener(null)
        holder.binding.cbGoal.isChecked = goal.isCompleted

        holder.binding.cbGoal.setOnCheckedChangeListener { _, isChecked ->
            if (goal.isCompleted != isChecked) {
                onGoalCheckedChange(goal, isChecked)
            }
        }

        holder.binding.btnDeleteGoal.setOnClickListener {
            onGoalDelete(goal)
        }

        holder.binding.root.setOnClickListener {
            val newChecked = !goal.isCompleted
            holder.binding.cbGoal.isChecked = newChecked
            onGoalCheckedChange(goal, newChecked)
        }
    }

    override fun getItemCount(): Int = goals.size

    fun updateGoals(newGoals: List<Goal>) {
        goals.clear()
        goals.addAll(newGoals)
        notifyDataSetChanged()
    }
}