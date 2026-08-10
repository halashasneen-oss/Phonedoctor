package com.screenlens.app.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.screenlens.app.R
import com.screenlens.app.domain.HistoryType
import java.text.DateFormat
import java.util.Date

@DrawableRes
fun HistoryType.iconRes(): Int = when (this) {
    HistoryType.SCREEN -> R.drawable.ic_type_screen
    HistoryType.IMAGE -> R.drawable.ic_type_image
    HistoryType.CAMERA -> R.drawable.ic_type_camera
    HistoryType.CLIPBOARD -> R.drawable.ic_type_clipboard
    HistoryType.QR -> R.drawable.ic_type_qr
    HistoryType.TRANSLATION -> R.drawable.ic_onboarding_translate
}

@StringRes
fun HistoryType.labelRes(): Int = when (this) {
    HistoryType.SCREEN -> R.string.filter_screen
    HistoryType.IMAGE -> R.string.filter_image
    HistoryType.CAMERA -> R.string.filter_camera
    HistoryType.CLIPBOARD -> R.string.tool_clipboard
    HistoryType.QR -> R.string.filter_qr
    HistoryType.TRANSLATION -> R.string.filter_translation
}

fun formatHistoryDate(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))
