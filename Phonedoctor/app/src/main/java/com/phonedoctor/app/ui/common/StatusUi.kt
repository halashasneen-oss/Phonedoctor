package com.phonedoctor.app.ui.common

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.phonedoctor.app.R
import com.phonedoctor.app.domain.model.TestStatus

/** Maps a [TestStatus] to the label / pill background every screen uses to render it. */
data class StatusUiModel(
    @StringRes val labelRes: Int,
    @DrawableRes val pillBackgroundRes: Int,
    @ColorRes val textColorRes: Int
)

fun TestStatus.toUiModel(): StatusUiModel = when (this) {
    TestStatus.EXCELLENT -> StatusUiModel(R.string.home_status_excellent, R.drawable.bg_pill_success, R.color.color_success)
    TestStatus.GOOD -> StatusUiModel(R.string.home_status_good, R.drawable.bg_pill_success, R.color.color_success)
    TestStatus.FAIR -> StatusUiModel(R.string.home_status_fair, R.drawable.bg_pill_warning, R.color.color_warning)
    TestStatus.POOR -> StatusUiModel(R.string.home_status_poor, R.drawable.bg_pill_danger, R.color.color_danger)
    TestStatus.UNAVAILABLE -> StatusUiModel(R.string.home_status_unknown, R.drawable.bg_pill_neutral, R.color.color_text_secondary)
}
