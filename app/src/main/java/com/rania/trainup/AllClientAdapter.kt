package com.rania.trainup

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rania.trainup.databinding.ItemActiveClientsTrainerBinding

class AllClientAdapter(
    private val clients: MutableList<Client>,
    private val onClientClick: (Client) -> Unit
) : RecyclerView.Adapter<AllClientAdapter.ClientViewHolder>() {

    class ClientViewHolder(val binding: ItemActiveClientsTrainerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientViewHolder {
        val binding = ItemActiveClientsTrainerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ClientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClientViewHolder, position: Int) {
        val client = clients[position]
        holder.binding.tvClientName.text = client.name
        holder.binding.tvClientEmail.text = client.email
        holder.binding.root.setOnClickListener {
            onClientClick(client)
        }
    }

    override fun getItemCount(): Int = clients.size

    fun updateClients(newClients: List<Client>) {
        clients.clear()
        clients.addAll(newClients)
        notifyDataSetChanged()
    }
}