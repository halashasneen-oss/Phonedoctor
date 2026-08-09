package com.phonedoctor.app.ui.common

import androidx.annotation.StringRes
import com.phonedoctor.app.R
import com.phonedoctor.app.domain.model.CategoryResult
import com.phonedoctor.app.domain.model.DiagnosticCategory
import com.phonedoctor.app.domain.model.TestStatus

/** Human-readable issue text for a category result that is FAIR/POOR, or null if it's not a problem. */
@StringRes
fun CategoryResult.issueMessageRes(): Int? {
    if (!status.isProblem) return null
    return when (category) {
        DiagnosticCategory.STORAGE -> R.string.results_issue_storage_full
        DiagnosticCategory.BATTERY -> R.string.results_issue_battery_drain
        DiagnosticCategory.MEMORY -> R.string.results_issue_memory_high
        DiagnosticCategory.CONNECTIVITY -> if (status == TestStatus.POOR) R.string.results_issue_no_connectivity else null
        else -> null
    }
}
