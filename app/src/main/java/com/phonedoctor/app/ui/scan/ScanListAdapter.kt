package com.phonedoctor.app.ui.scan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.phonedoctor.app.R
import com.phonedoctor.app.databinding.ItemScanRowBinding

class ScanListAdapter : RecyclerView.Adapter<ScanListAdapter.ViewHolder>() {

    private var items: List<ScanListItem> = emptyList()

    fun submitList(newItems: List<ScanListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemScanRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScanRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.textLabel.setText(item.labelRes)
        when (item.state) {
            ScanItemState.PENDING -> {
                holder.binding.imageStatus.visibility = View.VISIBLE
                holder.binding.progressRunning.visibility = View.GONE
                holder.binding.imageStatus.setImageResource(R.drawable.ic_close)
                holder.binding.imageStatus.alpha = 0.25f
                holder.binding.textLabel.alpha = 0.5f
            }
            ScanItemState.RUNNING -> {
                holder.binding.imageStatus.visibility = View.GONE
                holder.binding.progressRunning.visibility = View.VISIBLE
                holder.binding.textLabel.alpha = 1f
            }
            ScanItemState.DONE -> {
                holder.binding.imageStatus.visibility = View.VISIBLE
                holder.binding.progressRunning.visibility = View.GONE
                holder.binding.imageStatus.setImageResource(R.drawable.ic_check_circle)
                holder.binding.imageStatus.alpha = 1f
                holder.binding.imageStatus.setColorFilter(
                    holder.binding.root.context.getColor(R.color.color_success)
                )
                holder.binding.textLabel.alpha = 1f
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
