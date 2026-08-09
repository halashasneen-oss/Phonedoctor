package com.phonedoctor.app.data.repository

import com.phonedoctor.app.data.local.dao.ScanHistoryDao
import com.phonedoctor.app.data.local.entity.ScanHistoryEntity
import com.phonedoctor.app.domain.model.ScanReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepository(private val dao: ScanHistoryDao) {

    val history: Flow<List<ScanReport>> = dao.observeAll().map { list -> list.map { it.toReport() } }

    val latest: Flow<ScanReport?> = dao.observeLatest().map { it?.toReport() }

    suspend fun save(report: ScanReport): Long {
        return dao.insert(
            ScanHistoryEntity(
                timestampMillis = report.timestampMillis,
                healthScore = report.healthScore,
                results = report.results
            )
        )
    }

    suspend fun getById(id: Long): ScanReport? = dao.getById(id)?.toReport()

    suspend fun clearAll() = dao.clearAll()

    private fun ScanHistoryEntity.toReport() = ScanReport(
        id = id,
        timestampMillis = timestampMillis,
        healthScore = healthScore,
        results = results
    )
}
