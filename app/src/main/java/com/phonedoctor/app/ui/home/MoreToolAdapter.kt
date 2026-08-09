package com.phonedoctor.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.phonedoctor.app.databinding.ItemMoreToolBinding

class MoreToolAdapter(
    private val items: List<MoreToolItem>,
    private val onClick: (MoreToolItem) -> Unit
) : RecyclerView.Adapter<MoreToolAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemMoreToolBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMoreToolBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.imageIcon.setImageResource(item.iconRes)
        holder.binding.textTitle.setText(item.titleRes)
        holder.binding.root.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size
}
