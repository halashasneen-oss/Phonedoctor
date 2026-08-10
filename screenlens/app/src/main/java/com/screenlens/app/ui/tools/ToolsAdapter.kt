package com.screenlens.app.ui.tools

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.screenlens.app.databinding.ItemToolCardBinding

class ToolsAdapter(private val onClick: (ToolItem) -> Unit) : RecyclerView.Adapter<ToolsAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemToolCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemToolCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = toolItems[position]
        holder.binding.toolIcon.setImageResource(item.iconRes)
        holder.binding.toolTitle.setText(item.titleRes)
        holder.binding.toolDescription.setText(item.descriptionRes)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = toolItems.size
}
