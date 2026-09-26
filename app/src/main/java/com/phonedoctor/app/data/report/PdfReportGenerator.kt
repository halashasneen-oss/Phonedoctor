package com.phonedoctor.app.data.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import com.phonedoctor.app.R
import com.phonedoctor.app.domain.model.CategoryResult
import com.phonedoctor.app.domain.model.DiagnosticConfidence
import com.phonedoctor.app.domain.model.DiagnosticEvidenceType
import com.phonedoctor.app.domain.model.ScanMode
import com.phonedoctor.app.domain.model.ScanReport
import com.phonedoctor.app.domain.model.ScoreImpact
import com.phonedoctor.app.domain.util.FormatUtils
import com.phonedoctor.app.ui.common.toUiModel
import java.io.File
import java.io.FileOutputStream

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 80

    fun generate(
        context: Context,
        report: ScanReport
    ): File {
        val document = PdfDocument()
        val writer = PdfWriter(document)
        val performance = report.scanMode == ScanMode.PERFORMANCE

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 22f
            isFakeBoldText = true
        }
        val headingPaint = Paint().apply {
            color = Color.BLACK
            textSize = 13f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
        }
        val secondaryPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
        }
        val evidencePaint = Paint().apply {
            color = Color.rgb(0, 90, 120)
            textSize = 9f
        }
        val scorePaint = Paint().apply {
            color = Color.BLACK
            textSize = 32f
            isFakeBoldText = true
        }

        writer.line(
            context.getString(
                if (performance) {
                    R.string.report_performance_title
                } else {
                    R.string.report_title
                }
            ),
            titlePaint,
            30f
        )
        writer.line(
            "${context.getString(R.string.report_device)}: " +
                "${Build.MANUFACTURER} ${Build.MODEL}",
            secondaryPaint
        )
        writer.line(
            "${context.getString(R.string.report_date)}: " +
                FormatUtils.formatDateTime(report.timestampMillis),
            secondaryPaint
        )
        writer.line(
            "${context.getString(R.string.report_scan_type)}: " +
                context.getString(scanModeLabel(report.scanMode)),
            secondaryPaint,
            20f
        )

        if (performance) {
            writer.paragraph(
                context.getString(R.string.report_performance_summary),
                bodyPaint,
                24f
            )
        } else {
            writer.line(
                "${report.healthScore}%",
                scorePaint,
                38f
            )
            writer.line(
                context.getString(R.string.report_overall_health),
                secondaryPaint,
                24f
            )
        }

        writer.divider()

        report.results.forEach { result ->
            drawResult(
                context = context,
                writer = writer,
                result = result,
                headingPaint = headingPaint,
                bodyPaint = bodyPaint,
                secondaryPaint = secondaryPaint,
                evidencePaint = evidencePaint
            )
        }

        writer.divider()
        writer.paragraph(
            context.getString(R.string.privacy_intro),
            secondaryPaint,
            14f
        )

        writer.finish()

        val reportsDir = File(
            context.cacheDir,
            "reports"
        ).apply {
            mkdirs()
        }
        val file = File(
            reportsDir,
            "phone_doctor_report_${report.timestampMillis}.pdf"
        )
        FileOutputStream(file).use {
            document.writeTo(it)
        }
        document.close()
        return file
    }

    private fun drawResult(
        context: Context,
        writer: PdfWriter,
        result: CategoryResult,
        headingPaint: Paint,
        bodyPaint: Paint,
        secondaryPaint: Paint,
        evidencePaint: Paint
    ) {
        val categoryLabel = context.getString(
            result.category.toUiModel().labelRes
        )
        val statusLabel = context.getString(
            result.status.toUiModel().labelRes
        )

        writer.ensureSpace(82f)
        writer.line(
            "$categoryLabel — $statusLabel",
            headingPaint,
            17f
        )
        writer.paragraph(
            result.summary,
            bodyPaint,
            14f
        )
        result.detail?.takeIf { it.isNotBlank() }?.let {
            writer.paragraph(
                it,
                secondaryPaint,
                13f
            )
        }
        writer.paragraph(
            context.getString(
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
            ),
            evidencePaint,
            18f
        )
    }

    private fun scanModeLabel(mode: ScanMode): Int = when (mode) {
        ScanMode.QUICK -> R.string.results_mode_quick
        ScanMode.DEEP -> R.string.results_mode_deep
        ScanMode.PERFORMANCE -> R.string.results_mode_performance
        ScanMode.BACKGROUND -> R.string.results_mode_background
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

    private fun impactLabel(
        impact: ScoreImpact
    ): Int = when (impact) {
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

    private class PdfWriter(
        private val document: PdfDocument
    ) {
        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = MARGIN

        init {
            startPage()
        }

        fun ensureSpace(height: Float) {
            if (y + height <= PAGE_HEIGHT - MARGIN) return
            nextPage()
        }

        fun line(
            text: String,
            paint: Paint,
            afterSpacing: Float = 16f
        ) {
            val lines = wrap(text, paint)
            lines.forEach { value ->
                ensureSpace(paint.textSize + 8f)
                canvas?.drawText(
                    value,
                    MARGIN,
                    y,
                    paint
                )
                y += paint.textSize + 4f
            }
            y += afterSpacing.coerceAtLeast(0f)
        }

        fun paragraph(
            text: String,
            paint: Paint,
            afterSpacing: Float = 12f
        ) {
            line(text, paint, afterSpacing)
        }

        fun divider() {
            ensureSpace(22f)
            val dividerPaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
            }
            canvas?.drawLine(
                MARGIN,
                y,
                PAGE_WIDTH - MARGIN,
                y,
                dividerPaint
            )
            y += 22f
        }

        fun finish() {
            finishCurrentPage()
        }

        private fun nextPage() {
            finishCurrentPage()
            startPage()
        }

        private fun startPage() {
            pageNumber++
            val pageInfo = PdfDocument.PageInfo.Builder(
                PAGE_WIDTH,
                PAGE_HEIGHT,
                pageNumber
            ).create()
            page = document.startPage(pageInfo)
            canvas = page?.canvas
            y = MARGIN
        }

        private fun finishCurrentPage() {
            page?.let { document.finishPage(it) }
            page = null
            canvas = null
        }

        private fun wrap(
            text: String,
            paint: Paint
        ): List<String> {
            if (text.isBlank()) return listOf("")

            val output = mutableListOf<String>()
            text.lines().forEach { sourceLine ->
                if (sourceLine.isBlank()) {
                    output += ""
                    return@forEach
                }

                var current = StringBuilder()
                sourceLine.split(" ").forEach { word ->
                    val candidate = if (current.isEmpty()) {
                        word
                    } else {
                        "$current $word"
                    }

                    if (
                        current.isNotEmpty() &&
                        paint.measureText(candidate) > CONTENT_WIDTH
                    ) {
                        output += current.toString()
                        current = StringBuilder(word)
                    } else {
                        current = StringBuilder(candidate)
                    }
                }

                if (current.isNotEmpty()) {
                    output += current.toString()
                }
            }
            return output
        }
    }
}
