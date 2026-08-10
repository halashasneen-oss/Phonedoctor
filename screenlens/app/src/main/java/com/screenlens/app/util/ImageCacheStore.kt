package com.screenlens.app.util

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Writes a bitmap to a private cache subfolder (never external/shared storage) so it
 * can be passed to another Activity via a content:// URI. Callers are expected to
 * delete the file once they're done decoding it — nothing here is kept permanently.
 */
object ImageCacheStore {

    fun save(context: Context, bitmap: Bitmap): android.net.Uri {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "capture_${UUID.randomUUID()}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
