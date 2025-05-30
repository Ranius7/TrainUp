import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rania.trainup.RoutineDay
import com.rania.trainup.databinding.ItemRoutineDayBinding

class RoutineDayAdapter(
    private val routineDays: List<RoutineDay>,
    private val isTrainer: Boolean,
    private val onClick: (RoutineDay) -> Unit,
    private val onEditClick: ((RoutineDay) -> Unit)? = null,
    private val onDeleteClick: ((RoutineDay) -> Unit)? = null
) : RecyclerView.Adapter<RoutineDayAdapter.RoutineDayViewHolder>() {

    inner class RoutineDayViewHolder(val binding: ItemRoutineDayBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoutineDayViewHolder {
        val binding = ItemRoutineDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoutineDayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoutineDayViewHolder, position: Int) {
        val routineDay = routineDays[position]
        holder.binding.tvMuscleGroup.text = routineDay.muscleGroup.uppercase()

        if (!routineDay.comment.isNullOrBlank()) {
            holder.binding.tvComment.text = routineDay.comment
            holder.binding.tvComment.visibility = View.VISIBLE
        } else {
            holder.binding.tvComment.visibility = View.GONE
        }

        val numEjercicios = routineDay.exercises.size
        val numSeries = routineDay.exercises.sumOf { it.series }
        holder.binding.tvDetails.text = "$numEjercicios ejercicios · $numSeries series"

        if (isTrainer) {
            holder.binding.btnEditRoutineDay.visibility = View.VISIBLE
            holder.binding.btnDeleteRoutineDay.visibility = View.VISIBLE
            holder.binding.btnEditRoutineDay.setOnClickListener { onEditClick?.invoke(routineDay) }
            holder.binding.btnDeleteRoutineDay.setOnClickListener { onDeleteClick?.invoke(routineDay) }
        } else {
            holder.binding.btnEditRoutineDay.visibility = View.GONE
            holder.binding.btnDeleteRoutineDay.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onClick(routineDay) }
    }

    override fun getItemCount(): Int = routineDays.size
}