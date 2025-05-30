package com.rania.trainup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rania.trainup.com.rania.trainup.DailyTask
import com.rania.trainup.databinding.ItemDailyTaskBinding

class DailyTaskAdapter(
    private val tasks: MutableList<DailyTask>,
    private val onCheckedChange: (DailyTask, Boolean) -> Unit,
    private val onEdit: ((DailyTask) -> Unit)? = null,
    private val onDelete: ((DailyTask) -> Unit)? = null
) : RecyclerView.Adapter<DailyTaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(val binding: ItemDailyTaskBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemDailyTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.binding.tvTask.text = task.title
        holder.binding.cbTask.setOnCheckedChangeListener(null)
        holder.binding.cbTask.isChecked = task.isCompleted

        holder.binding.cbTask.setOnCheckedChangeListener { _, isChecked ->
            onCheckedChange(task, isChecked)
            if (isChecked) {
                val index = holder.adapterPosition
                if (index != RecyclerView.NO_POSITION) {
                    tasks.removeAt(index)
                    notifyItemRemoved(index)
                }
            }
        }


        holder.binding.btnEditTask?.apply {
            visibility = if (onEdit != null) View.VISIBLE else View.GONE
            setOnClickListener { onEdit?.invoke(task) }
        }
        holder.binding.btnDeleteTask?.apply {
            visibility = if (onDelete != null) View.VISIBLE else View.GONE
            setOnClickListener { onDelete?.invoke(task) }
        }
    }

    override fun getItemCount(): Int = tasks.size

    fun updateTasks(newTasks: List<DailyTask>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
    }
}