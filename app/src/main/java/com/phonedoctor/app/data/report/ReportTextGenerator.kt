package com.phonedoctor.app.data.report

import android.content.Context
import com.phonedoctor.app.R
import com.phonedoctor.app.domain.model.ScanReport
import com.phonedoctor.app.domain.util.FormatUtils
import com.phonedoctor.app.ui.common.toUiModel

object ReportTextGenerator {

    fun generate(context: Context, report: ScanReport, deviceLabel: String): String {
        val sb = StringBuilder()
        sb.appendLine(context.getString(R.string.report_title))
        sb.appendLine()
        sb.appendLine("${context.getString(R.string.report_device)}: $deviceLabel")
        sb.appendLine("${context.getString(R.string.report_date)}: ${FormatUtils.formatDateTime(report.timestampMillis)}")
        sb.appendLine("${context.getString(R.string.report_overall_health)}: ${report.healthScore}%")
        sb.appendLine()
        report.results.forEach { result ->
            val categoryLabel = context.getString(result.category.toUiModel().labelRes)
            val statusLabel = context.getString(result.status.toUiModel().labelRes)
            sb.appendLine("- $categoryLabel: $statusLabel (${result.summary})")
        }
        sb.appendLine()
        sb.appendLine(context.getString(R.string.privacy_intro))
        return sb.toString()
    }
}
