package com.phonedoctor.app.data.local.entity

import com.phonedoctor.app.domain.model.CategoryResult
import com.phonedoctor.app.domain.model.DiagnosticCategory
import com.phonedoctor.app.domain.model.DiagnosticConfidence
import com.phonedoctor.app.domain.model.DiagnosticEvidenceType
import com.phonedoctor.app.domain.model.ScoreImpact
import com.phonedoctor.app.domain.model.TestStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryResultListConverterTest {

    private val converter = CategoryResultListConverter()

    @Test
    fun `round trip preserves diagnostic engine 2 metadata`() {
        val results = listOf(
            CategoryResult(
                category = DiagnosticCategory.BATTERY,
                status = TestStatus.EXCELLENT,
                summary = "92%",
                detail = "Good",
                evidenceType = DiagnosticEvidenceType.MEASURED,
                scoreImpact = ScoreImpact.HEALTH,
                confidence = DiagnosticConfidence.HIGH
            ),
            CategoryResult(
                category = DiagnosticCategory.STORAGE,
                status = TestStatus.FAIR,
                summary = "88% used",
                detail = "Capacity state",
                evidenceType = DiagnosticEvidenceType.CURRENT_STATE,
                scoreImpact = ScoreImpact.INFORMATIONAL,
                confidence = DiagnosticConfidence.HIGH
            ),
            CategoryResult(
                category = DiagnosticCategory.CAMERA,
                status = TestStatus.UNAVAILABLE,
                summary = "No camera detected",
                evidenceType = DiagnosticEvidenceType.CAPABILITY,
                scoreImpact = ScoreImpact.INFORMATIONAL,
                confidence = DiagnosticConfidence.HIGH
            )
        )

        val serialized = converter.fromList(results)
        val restored = converter.toList(serialized)

        assertEquals(results, restored)
    }

    @Test
    fun `legacy four-field records remain readable`() {
        val recordSeparator = ""
        val unitSeparator = ""
        val raw = listOf(
            listOf(
                DiagnosticCategory.BATTERY.name,
                TestStatus.EXCELLENT.name,
                "92%",
                "Charging"
            ).joinToString(unitSeparator),
            listOf(
                DiagnosticCategory.STORAGE.name,
                TestStatus.FAIR.name,
                "88% used",
                ""
            ).joinToString(unitSeparator)
        ).joinToString(recordSeparator)

        val restored = converter.toList(raw)

        assertEquals(2, restored.size)
        assertEquals(DiagnosticEvidenceType.MEASURED, restored[0].evidenceType)
        assertEquals(ScoreImpact.HEALTH, restored[0].scoreImpact)
        assertEquals(DiagnosticConfidence.HIGH, restored[0].confidence)
        assertEquals("Charging", restored[0].detail)
        assertEquals(null, restored[1].detail)
    }

    @Test
    fun `empty list round trips to empty list`() {
        assertEquals(emptyList<CategoryResult>(), converter.toList(converter.fromList(emptyList())))
    }

    @Test
    fun `blank string decodes to empty list`() {
        assertEquals(emptyList<CategoryResult>(), converter.toList(""))
    }
}
