package com.phonedoctor.app.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.phonedoctor.app.R
import com.phonedoctor.app.domain.model.DiagnosticCategory

data class CategoryUiModel(@DrawableRes val iconRes: Int, @StringRes val labelRes: Int)

fun DiagnosticCategory.toUiModel(): CategoryUiModel = when (this) {
    DiagnosticCategory.BATTERY -> CategoryUiModel(R.drawable.ic_battery, R.string.category_battery)
    DiagnosticCategory.STORAGE -> CategoryUiModel(R.drawable.ic_storage, R.string.category_storage)
    DiagnosticCategory.MEMORY -> CategoryUiModel(R.drawable.ic_memory, R.string.category_memory)
    DiagnosticCategory.CPU -> CategoryUiModel(R.drawable.ic_cpu, R.string.category_cpu)
    DiagnosticCategory.DISPLAY -> CategoryUiModel(R.drawable.ic_display, R.string.category_display)
    DiagnosticCategory.TOUCH -> CategoryUiModel(R.drawable.ic_touch, R.string.category_touch)
    DiagnosticCategory.AUDIO -> CategoryUiModel(R.drawable.ic_speaker, R.string.category_audio)
    DiagnosticCategory.MICROPHONE -> CategoryUiModel(R.drawable.ic_microphone, R.string.category_microphone)
    DiagnosticCategory.SENSORS -> CategoryUiModel(R.drawable.ic_sensors, R.string.category_sensors)
    DiagnosticCategory.CAMERA -> CategoryUiModel(R.drawable.ic_camera, R.string.category_camera)
    DiagnosticCategory.CONNECTIVITY -> CategoryUiModel(R.drawable.ic_connectivity, R.string.category_connectivity)
}
