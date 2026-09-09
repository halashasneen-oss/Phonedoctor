package com.phonedoctor.app.data.repository

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.phonedoctor.app.domain.model.StorageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StorageRepository(@Suppress("UNUSED_PARAMETER") context: Context) {

    suspend fun getStorageInfo(): StorageInfo = withContext(Dispatchers.IO) {
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.totalBytes
        val freeBytes = stat.availableBytes
        val usedBytes = totalBytes - freeBytes
        StorageInfo(totalBytes = totalBytes, freeBytes = freeBytes, usedBytes = usedBytes)
    }
}
