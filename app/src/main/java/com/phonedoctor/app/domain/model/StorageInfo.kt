package com.phonedoctor.app.domain.model

data class StorageBreakdown(
    val imagesBytes: Long,
    val videosBytes: Long,
    val audioBytes: Long,
    val documentsBytes: Long,
    val appsBytes: Long,
    val otherBytes: Long
)

data class StorageInfo(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val breakdown: StorageBreakdown? = null
)
