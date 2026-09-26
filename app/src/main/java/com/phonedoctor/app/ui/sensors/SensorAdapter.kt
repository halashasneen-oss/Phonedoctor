package com.phonedoctor.app.ui.sensors

import android.hardware.Sensor
import android.hardware.SensorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.phonedoctor.app.R
import com.phonedoctor.app.databinding.ItemSensorRowBinding
import com.phonedoctor.app.domain.model.SensorEntry
import com.phonedoctor.app.domain.model.SensorKind

data class SensorLiveUi(
    val reading: String,
    val accuracy: Int,
    val sampleRateHz: Double?
)

class SensorAdapter : RecyclerView.Adapter<SensorAdapter.ViewHolder>() {

    private var items: List<SensorEntry> = emptyList()
    private val live = mutableMapOf<SensorKind, SensorLiveUi>()

    fun submitList(newItems: List<SensorEntry>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateReading(kind: SensorKind, reading: SensorLiveUi) {
        live[kind] = reading
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
        val context = holder.binding.root.context

        holder.binding.textLabel.setText(item.kind.labelRes())
        if (!item.available) {
            holder.binding.textStatus.setText(R.string.sensor_not_available)
            holder.binding.textStatus.setBackgroundResource(R.drawable.bg_pill_neutral)
            holder.binding.textStatus.setTextColor(context.getColor(R.color.color_text_secondary))
            holder.binding.textReading.visibility = View.GONE
            holder.binding.textMetadata.visibility = View.GONE
            return
        }

        holder.binding.textStatus.setText(R.string.sensor_available)
        holder.binding.textStatus.setBackgroundResource(R.drawable.bg_pill_success)
        holder.binding.textStatus.setTextColor(context.getColor(R.color.color_success))
        holder.binding.textReading.visibility = View.VISIBLE
        holder.binding.textMetadata.visibility = View.VISIBLE

        val liveReading = live[item.kind]
        holder.binding.textReading.text = liveReading?.let {
            buildString {
                append(it.reading)
                append(" · ")
                append(context.getString(accuracyLabel(it.accuracy)))
                it.sampleRateHz?.let { hz ->
                    append(" · ")
                    append("%.1f Hz".format(hz))
                }
            }
        } ?: "…"

        holder.binding.textMetadata.text = buildString {
            item.displayName?.let {
                append(it)
                item.vendor?.let { vendor -> append(" · ").append(vendor) }
                append("\n")
            }
            item.resolution?.let { append("Resolution: ").append(it).append(" · ") }
            item.maximumRange?.let { append("Range: ").append(it).append(" · ") }
            item.powerMilliAmps?.let { append("Power: ").append(it).append(" mA") }
            append("\n")
            item.minDelayMicroseconds?.let {
                append("Min delay: ").append(it).append(" µs · ")
            }
            item.reportingMode?.let {
                append(reportingModeLabel(it)).append(" · ")
            }
            item.wakeUpSensor?.let {
                append(if (it) "Wake-up" else "Non wake-up")
            }
            append("\n")
            append("FIFO: ")
            append(item.fifoReservedEventCount ?: 0)
            append("/")
            append(item.fifoMaxEventCount ?: 0)
            item.version?.let { append(" · v").append(it) }
        }.trim()
    }

    override fun getItemCount(): Int = items.size

    private fun accuracyLabel(accuracy: Int): Int = when (accuracy) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> R.string.sensor_accuracy_high
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> R.string.sensor_accuracy_medium
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> R.string.sensor_accuracy_low
        else -> R.string.sensor_accuracy_unreliable
    }

    private fun reportingModeLabel(mode: Int): String = when (mode) {
        Sensor.REPORTING_MODE_CONTINUOUS -> "Continuous"
        Sensor.REPORTING_MODE_ON_CHANGE -> "On-change"
        Sensor.REPORTING_MODE_ONE_SHOT -> "One-shot"
        Sensor.REPORTING_MODE_SPECIAL_TRIGGER -> "Special trigger"
        else -> "Mode $mode"
    }
}
