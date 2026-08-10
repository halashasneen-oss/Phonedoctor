package com.screenlens.app.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.screenlens.app.databinding.ItemHistoryCardBinding
import com.screenlens.app.domain.HistoryItem

class HistoryItemAdapter(private val onClick: (HistoryItem) -> Unit) :
    ListAdapter<HistoryItem, HistoryItemAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(val binding: ItemHistoryCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val context = holder.itemView.context
        holder.binding.typeIcon.setImageResource(item.type.iconRes())
        holder.binding.originalText.text = item.originalText
        holder.binding.subtitle.text = context.getString(item.type.labelRes()) +
            " · " + formatHistoryDate(item.createdAt)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<HistoryItem>() {
            override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem) = oldItem == newItem
        }
    }
}
