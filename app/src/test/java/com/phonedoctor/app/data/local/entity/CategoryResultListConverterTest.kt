package com.phonedoctor.app.data.local.entity

import com.phonedoctor.app.domain.model.CategoryResult
import com.phonedoctor.app.domain.model.DiagnosticCategory
import com.phonedoctor.app.domain.model.TestStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryResultListConverterTest {

    private val converter = CategoryResultListConverter()

    @Test
    fun `round trip preserves all fields`() {
        val results = listOf(
            CategoryResult(DiagnosticCategory.BATTERY, TestStatus.EXCELLENT, "92%", "Charging"),
            CategoryResult(DiagnosticCategory.STORAGE, TestStatus.FAIR, "88% used", null),
            CategoryResult(DiagnosticCategory.CAMERA, TestStatus.UNAVAILABLE, "No camera detected")
        )

        val serialized = converter.fromList(results)
        val restored = converter.toList(serialized)

        assertEquals(results, restored)
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
