package com.phonedoctor.app.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.phonedoctor.app.R
import com.phonedoctor.app.databinding.ItemHistoryRowBinding
import com.phonedoctor.app.domain.model.ScanReport
import com.phonedoctor.app.domain.util.FormatUtils

class HistoryAdapter(
    private val onClick: (ScanReport) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private var items: List<ScanReport> = emptyList()

    fun submitList(newItems: List<ScanReport>) {
        items = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemHistoryRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.binding.root.context
        holder.binding.textDate.text = FormatUtils.formatDateTime(item.timestampMillis)
        holder.binding.textHealth.text = "${item.healthScore}%"

        val parts = mutableListOf(context.getString(R.string.history_passed_fmt, item.passedCount))
        if (item.warningCount > 0) parts += context.getString(R.string.history_warnings_fmt, item.warningCount)
        if (item.failedCount > 0) parts += context.getString(R.string.history_failed_fmt, item.failedCount)
        holder.binding.textSummary.text = parts.joinToString("   ")

        holder.binding.root.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size
}
