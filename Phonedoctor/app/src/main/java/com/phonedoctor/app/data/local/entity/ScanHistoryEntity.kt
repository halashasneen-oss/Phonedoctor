package com.phonedoctor.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.phonedoctor.app.domain.model.CategoryResult
import com.phonedoctor.app.domain.model.DiagnosticCategory
import com.phonedoctor.app.domain.model.TestStatus

private const val RECORD_SEPARATOR = ""
private const val UNIT_SEPARATOR = ""

@Entity(tableName = "scan_history")
@TypeConverters(CategoryResultListConverter::class)
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestampMillis: Long,
    val healthScore: Int,
    val results: List<CategoryResult>
)

class CategoryResultListConverter {

    @TypeConverter
    fun fromList(results: List<CategoryResult>): String {
        return results.joinToString(RECORD_SEPARATOR) { r ->
            listOf(
                r.category.name,
                r.status.name,
                r.summary,
                r.detail.orEmpty()
            ).joinToString(UNIT_SEPARATOR)
        }
    }

    @TypeConverter
    fun toList(raw: String): List<CategoryResult> {
        if (raw.isBlank()) return emptyList()
        return raw.split(RECORD_SEPARATOR).mapNotNull { record ->
            val parts = record.split(UNIT_SEPARATOR)
            if (parts.size < 3) return@mapNotNull null
            val category = runCatching { DiagnosticCategory.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
            val status = runCatching { TestStatus.valueOf(parts[1]) }.getOrNull() ?: return@mapNotNull null
            CategoryResult(
                category = category,
                status = status,
                summary = parts[2],
                detail = parts.getOrNull(3)?.ifEmpty { null }
            )
        }
    }
}
