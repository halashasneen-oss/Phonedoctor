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

    class ViewHolder(val binding: ItemSensorRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSensorRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.binding.root.context

        holder.binding.textLabel.setText(item.kind.labelRes())
        if (!item.available) {
            holder.binding.textStatus.setText(R.string.sensor_not_available)
            holder.binding.textStatus.setBackgroundResource(R.drawable.bg_pill_neutral)
            holder.binding.textStatus.setTextColor(
                context.getColor(R.color.color_text_secondary)
            )
            holder.binding.textReading.visibility = View.GONE
            holder.binding.textMetadata.visibility = View.GONE
            return
        }

        holder.binding.textStatus.setText(R.string.sensor_available)
        holder.binding.textStatus.setBackgroundResource(R.drawable.bg_pill_success)
        holder.binding.textStatus.setTextColor(
            context.getColor(R.color.color_success)
        )
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
                    append(context.getString(R.string.sensor_sample_rate_fmt, hz))
                }
            }
        } ?: "…"

        holder.binding.textMetadata.text = buildString {
            item.displayName?.let { name ->
                append(name)
                item.vendor?.let { vendor -> append(" · ").append(vendor) }
                append("\n")
            }

            val primary = mutableListOf<String>()
            item.resolution?.let {
                primary += context.getString(R.string.sensor_resolution_fmt, it)
            }
            item.maximumRange?.let {
                primary += context.getString(R.string.sensor_range_fmt, it)
            }
            item.powerMilliAmps?.let {
                primary += context.getString(R.string.sensor_power_fmt, it)
            }
            append(primary.joinToString(" · "))

            val secondary = mutableListOf<String>()
            item.minDelayMicroseconds?.let {
                secondary += context.getString(R.string.sensor_min_delay_fmt, it)
            }
            item.reportingMode?.let {
                secondary += context.getString(reportingModeLabel(it))
            }
            item.wakeUpSensor?.let {
                secondary += context.getString(
                    if (it) {
                        R.string.sensor_wakeup
                    } else {
                        R.string.sensor_non_wakeup
                    }
                )
            }
            if (secondary.isNotEmpty()) {
                append("\n")
                append(secondary.joinToString(" · "))
            }

            append("\n")
            append(
                context.getString(
                    R.string.sensor_fifo_fmt,
                    item.fifoReservedEventCount ?: 0,
                    item.fifoMaxEventCount ?: 0
                )
            )
            item.version?.let {
                append(" · ")
                append(context.getString(R.string.sensor_version_fmt, it))
            }
        }.trim()
    }

    override fun getItemCount(): Int = items.size

    private fun accuracyLabel(accuracy: Int): Int = when (accuracy) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> R.string.sensor_accuracy_high
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> R.string.sensor_accuracy_medium
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> R.string.sensor_accuracy_low
        else -> R.string.sensor_accuracy_unreliable
    }

    private fun reportingModeLabel(mode: Int): Int = when (mode) {
        Sensor.REPORTING_MODE_CONTINUOUS -> R.string.sensor_mode_continuous
        Sensor.REPORTING_MODE_ON_CHANGE -> R.string.sensor_mode_on_change
        Sensor.REPORTING_MODE_ONE_SHOT -> R.string.sensor_mode_one_shot
        Sensor.REPORTING_MODE_SPECIAL_TRIGGER -> R.string.sensor_mode_special_trigger
        else -> R.string.sensor_mode_unknown
    }
}
