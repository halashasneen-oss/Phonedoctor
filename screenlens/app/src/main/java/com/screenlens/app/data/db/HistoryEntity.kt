package com.screenlens.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.screenlens.app.domain.HistoryItem
import com.screenlens.app.domain.HistoryType

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val type: String,
    val originalText: String,
    val translatedText: String?,
    val sourceLanguage: String?,
    val targetLanguage: String?,
    val detectedLanguage: String?
)

fun HistoryEntity.toDomain() = HistoryItem(
    id = id,
    createdAt = createdAt,
    type = runCatching { HistoryType.valueOf(type) }.getOrDefault(HistoryType.SCREEN),
    originalText = originalText,
    translatedText = translatedText,
    sourceLanguage = sourceLanguage,
    targetLanguage = targetLanguage,
    detectedLanguage = detectedLanguage
)

fun HistoryItem.toEntity() = HistoryEntity(
    id = id,
    createdAt = createdAt,
    type = type.name,
    originalText = originalText,
    translatedText = translatedText,
    sourceLanguage = sourceLanguage,
    targetLanguage = targetLanguage,
    detectedLanguage = detectedLanguage
)
