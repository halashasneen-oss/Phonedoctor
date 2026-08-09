package com.phonedoctor.app.domain.model

/**
 * Normalized outcome of any single diagnostic test or category. UNAVAILABLE is
 * distinct from FAIL: it means the platform did not expose enough information
 * to judge the component, not that the component is broken.
 */
enum class TestStatus {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    UNAVAILABLE;

    val isProblem: Boolean
        get() = this == FAIR || this == POOR
}
