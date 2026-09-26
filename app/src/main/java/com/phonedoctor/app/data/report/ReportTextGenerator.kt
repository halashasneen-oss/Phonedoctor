package com.phonedoctor.app.data.report

import android.content.Context
import com.phonedoctor.app.R
import com.phonedoctor.app.domain.model.ScanMode
import com.phonedoctor.app.domain.model.ScanReport
import com.phonedoctor.app.domain.util.FormatUtils
import com.phonedoctor.app.ui.common.toUiModel

object ReportTextGenerator {

    fun generate(
        context: Context,
        report: ScanReport,
        deviceLabel: String
    ): String {
        val performance = report.scanMode == ScanMode.PERFORMANCE
        val sb = StringBuilder()

        sb.appendLine(
            context.getString(
                if (performance) {
                    R.string.report_performance_title
                } else {
                    R.string.report_title
                }
            )
        )
        sb.appendLine()
        sb.appendLine(
            "${context.getString(R.string.report_device)}: $deviceLabel"
        )
        sb.appendLine(
            "${context.getString(R.string.report_date)}: " +
                FormatUtils.formatDateTime(report.timestampMillis)
        )
        sb.appendLine(
            "${context.getString(R.string.report_scan_type)}: " +
                context.getString(scanModeLabel(report.scanMode))
        )

        if (performance) {
            sb.appendLine(
                context.getString(R.string.report_performance_summary)
            )
        } else {
            sb.appendLine(
                "${context.getString(R.string.report_overall_health)}: " +
                    "${report.healthScore}%"
            )
        }

        sb.appendLine()
        report.results.forEach { result ->
            val categoryLabel = context.getString(
                result.category.toUiModel().labelRes
            )
            val statusLabel = context.getString(
                result.status.toUiModel().labelRes
            )
            sb.appendLine(
                "- $categoryLabel: $statusLabel (${result.summary})"
            )
            result.detail?.let {
                sb.appendLine("  $it")
            }
        }

        sb.appendLine()
        sb.appendLine(context.getString(R.string.privacy_intro))
        return sb.toString()
    }

    private fun scanModeLabel(mode: ScanMode): Int = when (mode) {
        ScanMode.QUICK -> R.string.results_mode_quick
        ScanMode.DEEP -> R.string.results_mode_deep
        ScanMode.PERFORMANCE -> R.string.results_mode_performance
        ScanMode.BACKGROUND -> R.string.results_mode_background
    }
}
