package com.phonedoctor.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.phonedoctor.app.data.local.dao.ScanHistoryDao
import com.phonedoctor.app.data.local.entity.CategoryResultListConverter
import com.phonedoctor.app.data.local.entity.ScanHistoryEntity

@Database(entities = [ScanHistoryEntity::class], version = 1, exportSchema = false)
@TypeConverters(CategoryResultListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao
}
