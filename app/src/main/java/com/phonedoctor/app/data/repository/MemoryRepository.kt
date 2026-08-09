package com.phonedoctor.app.data.repository

import android.app.ActivityManager
import android.content.Context
import com.phonedoctor.app.domain.model.MemoryInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MemoryRepository(private val context: Context) {

    suspend fun getMemoryInfo(): MemoryInfo = withContext(Dispatchers.IO) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        val used = info.totalMem - info.availMem
        MemoryInfo(
            totalBytes = info.totalMem,
            availableBytes = info.availMem,
            usedBytes = used,
            isLowMemory = info.lowMemory,
            lowMemoryThresholdBytes = info.threshold
        )
    }
}
