package com.phonedoctor.app.data.report

import android.content.Context
import com.phonedoctor.app.R
import com.phonedoctor.app.domain.model.DiagnosticConfidence
import com.phonedoctor.app.domain.model.DiagnosticEvidenceType
import com.phonedoctor.app.domain.model.ScanMode
import com.phonedoctor.app.domain.model.ScoreImpact
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
            sb.appendLine(
                "  " + context.getString(
                    R.string.result_evidence_fmt,
                    context.getString(
                        evidenceLabel(result.evidenceType)
                    ),
                    context.getString(
                        impactLabel(result.scoreImpact)
                    ),
                    context.getString(
                        confidenceLabel(result.confidence)
                    )
                )
            )
        }

        sb.appendLine()
        sb.appendLine(context.getString(R.string.privacy_intro))
        return sb.toString()
    }

    private fun evidenceLabel(
        type: DiagnosticEvidenceType
    ): Int = when (type) {
        DiagnosticEvidenceType.MEASURED -> R.string.result_evidence_measured
        DiagnosticEvidenceType.CAPABILITY -> R.string.result_evidence_capability
        DiagnosticEvidenceType.CURRENT_STATE -> R.string.result_evidence_current_state
        DiagnosticEvidenceType.USER_VERIFIED -> R.string.result_evidence_user_verified
        DiagnosticEvidenceType.ESTIMATED -> R.string.result_evidence_estimated
    }

    private fun impactLabel(impact: ScoreImpact): Int = when (impact) {
        ScoreImpact.HEALTH -> R.string.result_impact_health
        ScoreImpact.INFORMATIONAL -> R.string.result_impact_informational
    }

    private fun confidenceLabel(
        confidence: DiagnosticConfidence
    ): Int = when (confidence) {
        DiagnosticConfidence.HIGH -> R.string.result_confidence_high
        DiagnosticConfidence.MEDIUM -> R.string.result_confidence_medium
        DiagnosticConfidence.LOW -> R.string.result_confidence_low
    }

    private fun scanModeLabel(mode: ScanMode): Int = when (mode) {
        ScanMode.QUICK -> R.string.results_mode_quick
        ScanMode.DEEP -> R.string.results_mode_deep
        ScanMode.PERFORMANCE -> R.string.results_mode_performance
        ScanMode.BACKGROUND -> R.string.results_mode_background
    }
}
