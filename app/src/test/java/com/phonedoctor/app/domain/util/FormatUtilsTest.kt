package com.phonedoctor.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatUtilsTest {

    @Test
    fun `formatBytes handles zero`() {
        assertEquals("0 B", FormatUtils.formatBytes(0))
    }

    @Test
    fun `formatBytes uses appropriate unit`() {
        assertEquals("1.0 KB", FormatUtils.formatBytes(1024))
        assertEquals("1.0 MB", FormatUtils.formatBytes(1024L * 1024))
        assertEquals("1.0 GB", FormatUtils.formatBytes(1024L * 1024 * 1024))
    }

    @Test
    fun `formatRelativeTime reports just now for recent timestamps`() {
        val now = 1_700_000_000_000L
        assertEquals("Just now", FormatUtils.formatRelativeTime(now - 10_000, now))
    }

    @Test
    fun `formatRelativeTime reports minutes ago`() {
        val now = 1_700_000_000_000L
        assertEquals("5 min ago", FormatUtils.formatRelativeTime(now - 5 * 60_000, now))
    }
}
