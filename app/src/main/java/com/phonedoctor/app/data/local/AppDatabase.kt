package com.phonedoctor.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.phonedoctor.app.data.local.dao.ScanHistoryDao
import com.phonedoctor.app.data.local.entity.CategoryResultListConverter
import com.phonedoctor.app.data.local.entity.ScanHistoryEntity

@Database(
    entities = [ScanHistoryEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(CategoryResultListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE scan_history " +
                        "ADD COLUMN scanMode TEXT NOT NULL DEFAULT 'DEEP'"
                )
            }
        }
    }
}
