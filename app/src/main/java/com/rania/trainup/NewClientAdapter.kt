package com.rania.trainup

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rania.trainup.databinding.ItemNewClientsTrainerBinding

class NewClientAdapter(
    private val clients: MutableList<Client>,
    private val onClientClick: (Client) -> Unit
) : RecyclerView.Adapter<NewClientAdapter.NewClientViewHolder>() {

    class NewClientViewHolder(val binding: ItemNewClientsTrainerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewClientViewHolder {
        val binding = ItemNewClientsTrainerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NewClientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewClientViewHolder, position: Int) {
        val client = clients[position]
        holder.binding.tvClienteNombre.text = client.name
        holder.binding.tvClienteInfo.text = client.email
        holder.binding.root.setOnClickListener {
            onClientClick(client)
        }
    }

    override fun getItemCount(): Int = clients.size


}
