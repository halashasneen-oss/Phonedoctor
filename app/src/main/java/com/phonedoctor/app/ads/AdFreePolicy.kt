package com.phonedoctor.app.ads

object AdFreePolicy {
    const val REWARD_DURATION_MILLIS: Long = 60L * 60L * 1000L

    fun isAdFree(
        isPremium: Boolean,
        adFreeUntilMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean = isPremium || adFreeUntilMillis > nowMillis

    fun remainingMillis(
        adFreeUntilMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): Long = (adFreeUntilMillis - nowMillis).coerceAtLeast(0L)

    fun rewardExpiry(nowMillis: Long = System.currentTimeMillis()): Long =
        nowMillis + REWARD_DURATION_MILLIS
}
