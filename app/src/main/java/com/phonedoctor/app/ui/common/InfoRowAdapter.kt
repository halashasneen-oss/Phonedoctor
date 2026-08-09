package com.phonedoctor.app.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.phonedoctor.app.databinding.ItemInfoRowBinding

data class InfoRow(val label: String, val value: String)

class InfoRowAdapter : RecyclerView.Adapter<InfoRowAdapter.ViewHolder>() {

    private var items: List<InfoRow> = emptyList()

    fun submitList(newItems: List<InfoRow>) {
        items = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemInfoRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInfoRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.textLabel.text = item.label
        holder.binding.textValue.text = item.value
    }

    override fun getItemCount(): Int = items.size
}
