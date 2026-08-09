package com.phonedoctor.app.data.repository

import android.content.Context
import android.database.Cursor
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import com.phonedoctor.app.domain.model.StorageBreakdown
import com.phonedoctor.app.domain.model.StorageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StorageRepository(private val context: Context) {

    suspend fun getStorageInfo(): StorageInfo = withContext(Dispatchers.IO) {
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.totalBytes
        val freeBytes = stat.availableBytes
        val usedBytes = totalBytes - freeBytes
        StorageInfo(totalBytes = totalBytes, freeBytes = freeBytes, usedBytes = usedBytes)
    }

    /**
     * Requires READ_MEDIA_IMAGES/VIDEO/AUDIO (API 33+) or READ_EXTERNAL_STORAGE
     * (API <= 32) to already be granted. Callers must check permission first;
     * this never requests it.
     */
    suspend fun getStorageBreakdown(): StorageBreakdown = withContext(Dispatchers.IO) {
        StorageBreakdown(
            imagesBytes = sumMediaStoreSize(MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
            videosBytes = sumMediaStoreSize(MediaStore.Video.Media.EXTERNAL_CONTENT_URI),
            audioBytes = sumMediaStoreSize(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI),
            documentsBytes = sumMediaStoreSize(MediaStore.Files.getContentUri("external"), documentsOnly = true),
            appsBytes = sumInstalledAppsSize(),
            otherBytes = 0L
        )
    }

    private fun sumMediaStoreSize(uri: android.net.Uri, documentsOnly: Boolean = false): Long {
        return runCatching {
            val projection = arrayOf(MediaStore.MediaColumns.SIZE)
            val selection: String?
            val selectionArgs: Array<String>?
            if (documentsOnly) {
                selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
                selectionArgs = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_DOCUMENT.toString())
            } else {
                selection = null
                selectionArgs = null
            }
            var total = 0L
            val cursor: Cursor? = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            cursor?.use {
                val sizeIndex = it.getColumnIndex(MediaStore.MediaColumns.SIZE)
                if (sizeIndex >= 0) {
                    while (it.moveToNext()) {
                        total += it.getLong(sizeIndex)
                    }
                }
            }
            total
        }.getOrDefault(0L)
    }

    private fun sumInstalledAppsSize(): Long {
        // Per-app storage size requires the PACKAGE_USAGE_STATS special access
        // (StorageStatsManager#queryStatsForPackage), which cannot be granted
        // through a normal runtime permission dialog. Left as 0 to avoid an
        // inaccurate estimate rather than guessing.
        return 0L
    }
}
