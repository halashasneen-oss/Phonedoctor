package com.phonedoctor.app.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.phonedoctor.app.domain.model.TestStatus

data class QuickTestItem(
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
    @IdRes val navActionId: Int,
    val status: TestStatus,
    val statusText: String
)

data class MoreToolItem(
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
    @IdRes val navActionId: Int
)
