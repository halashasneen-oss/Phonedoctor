package com.phonedoctor.app.ui.sensors

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.phonedoctor.app.R
import com.phonedoctor.app.databinding.ItemSensorRowBinding
import com.phonedoctor.app.domain.model.SensorEntry
import com.phonedoctor.app.domain.model.SensorKind

class SensorAdapter : RecyclerView.Adapter<SensorAdapter.ViewHolder>() {

    private var items: List<SensorEntry> = emptyList()
    private val readings = mutableMapOf<SensorKind, String>()

    fun submitList(newItems: List<SensorEntry>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateReading(kind: SensorKind, text: String) {
        readings[kind] = text
        val index = items.indexOfFirst { it.kind == kind }
        if (index >= 0) notifyItemChanged(index)
    }

    class ViewHolder(val binding: ItemSensorRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSensorRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.textLabel.setText(item.kind.labelRes())
        if (item.available) {
            holder.binding.textStatus.setText(R.string.sensor_available)
            holder.binding.textStatus.setBackgroundResource(R.drawable.bg_pill_success)
            holder.binding.textStatus.setTextColor(holder.binding.root.context.getColor(R.color.color_success))
            holder.binding.textReading.visibility = View.VISIBLE
            holder.binding.textReading.text = readings[item.kind] ?: "…"
        } else {
            holder.binding.textStatus.setText(R.string.sensor_not_available)
            holder.binding.textStatus.setBackgroundResource(R.drawable.bg_pill_neutral)
            holder.binding.textStatus.setTextColor(holder.binding.root.context.getColor(R.color.color_text_secondary))
            holder.binding.textReading.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size
}
