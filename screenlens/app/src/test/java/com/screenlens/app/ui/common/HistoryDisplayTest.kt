package com.screenlens.app.ui.common

import com.screenlens.app.R
import com.screenlens.app.domain.HistoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class HistoryDisplayTest {

    @Test
    fun `every history type maps to a distinct icon`() {
        val icons = HistoryType.entries.map { it.iconRes() }
        assertEquals(icons.size, icons.toSet().size)
    }

    @Test
    fun `every history type has a label resource`() {
        HistoryType.entries.forEach { type ->
            assertNotEquals(0, type.labelRes())
        }
    }

    @Test
    fun `screen type uses the screen icon`() {
        assertEquals(R.drawable.ic_type_screen, HistoryType.SCREEN.iconRes())
    }

    @Test
    fun `formatHistoryDate does not throw and returns non-blank text`() {
        val formatted = formatHistoryDate(1_700_000_000_000L)
        assert(formatted.isNotBlank())
    }
}
