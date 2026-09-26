package com.phonedoctor.app.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdFreePolicyTest {

    @Test
    fun `reward lasts exactly one hour`() {
        val now = 1_700_000_000_000L
        assertEquals(now + 3_600_000L, AdFreePolicy.rewardExpiry(now))
    }

    @Test
    fun `temporary entitlement is active before expiry`() {
        val now = 10_000L
        assertTrue(AdFreePolicy.isAdFree(false, now + 1L, now))
        assertFalse(AdFreePolicy.isAdFree(false, now, now))
    }

    @Test
    fun `premium always disables ads`() {
        assertTrue(AdFreePolicy.isAdFree(true, 0L, 999_999L))
    }

    @Test
    fun `remaining time never becomes negative`() {
        assertEquals(0L, AdFreePolicy.remainingMillis(100L, 101L))
    }
}
