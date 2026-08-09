package com.phonedoctor.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.phonedoctor.app.databinding.ItemQuickTestBinding

class QuickTestAdapter(
    private val onClick: (QuickTestItem) -> Unit
) : RecyclerView.Adapter<QuickTestAdapter.ViewHolder>() {

    private var items: List<QuickTestItem> = emptyList()

    fun submitList(newItems: List<QuickTestItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemQuickTestBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQuickTestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.imageIcon.setImageResource(item.iconRes)
        holder.binding.textTitle.setText(item.titleRes)
        holder.binding.textStatus.text = item.statusText
        holder.binding.root.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size
}
