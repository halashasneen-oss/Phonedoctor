package com.phonedoctor.app.data.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import com.phonedoctor.app.R
import com.phonedoctor.app.domain.model.ScanReport
import com.phonedoctor.app.domain.util.FormatUtils
import com.phonedoctor.app.ui.common.toUiModel
import java.io.File
import java.io.FileOutputStream

/** Renders a [ScanReport] to a simple, real one-page PDF stored under the app cache dir. */
object PdfReportGenerator {

    private const val PAGE_WIDTH = 595 // A4 @ 72dpi
    private const val PAGE_HEIGHT = 842

    fun generate(context: Context, report: ScanReport): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply { color = Color.BLACK; textSize = 22f; isFakeBoldText = true }
        val labelPaint = Paint().apply { color = Color.DKGRAY; textSize = 11f }
        val bodyPaint = Paint().apply { color = Color.BLACK; textSize = 13f }
        val scorePaint = Paint().apply { color = Color.BLACK; textSize = 36f; isFakeBoldText = true }

        var y = 48f
        canvas.drawText(context.getString(R.string.report_title), 40f, y, titlePaint)

        y += 30f
        canvas.drawText("${context.getString(R.string.report_device)}: ${Build.MANUFACTURER} ${Build.MODEL}", 40f, y, labelPaint)
        y += 18f
        canvas.drawText("${context.getString(R.string.report_date)}: ${FormatUtils.formatDateTime(report.timestampMillis)}", 40f, y, labelPaint)

        y += 44f
        canvas.drawText("${report.healthScore}%", 40f, y, scorePaint)
        y += 20f
        canvas.drawText(context.getString(R.string.report_overall_health), 40f, y, labelPaint)

        y += 36f
        val lineY = y
        canvas.drawLine(40f, lineY, (PAGE_WIDTH - 40).toFloat(), lineY, Paint().apply { color = Color.LTGRAY })

        y += 28f
        report.results.forEach { result ->
            val categoryLabel = context.getString(result.category.toUiModel().labelRes)
            val statusLabel = context.getString(result.status.toUiModel().labelRes)
            canvas.drawText("$categoryLabel", 40f, y, bodyPaint)
            canvas.drawText("$statusLabel — ${result.summary}", 220f, y, labelPaint)
            y += 22f
        }

        y += 20f
        val privacyPaint = Paint().apply { color = Color.GRAY; textSize = 9f }
        drawWrappedText(canvas, context.getString(R.string.privacy_intro), 40f, y, PAGE_WIDTH - 80, privacyPaint)

        document.finishPage(page)

        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(reportsDir, "phone_doctor_report_${report.timestampMillis}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun drawWrappedText(canvas: Canvas, text: String, x: Float, startY: Float, maxWidth: Int, paint: Paint) {
        val words = text.split(" ")
        var line = StringBuilder()
        var y = startY
        for (word in words) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) > maxWidth) {
                canvas.drawText(line.toString(), x, y, paint)
                y += 12f
                line = StringBuilder(word)
            } else {
                line = StringBuilder(candidate)
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line.toString(), x, y, paint)
        }
    }
}
