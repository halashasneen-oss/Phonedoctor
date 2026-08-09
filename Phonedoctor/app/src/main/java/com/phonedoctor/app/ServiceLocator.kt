package com.phonedoctor.app

import android.content.Context
import androidx.room.Room
import com.phonedoctor.app.data.datastore.SettingsRepository
import com.phonedoctor.app.data.local.AppDatabase
import com.phonedoctor.app.data.repository.BatteryRepository
import com.phonedoctor.app.data.repository.ConnectivityRepository
import com.phonedoctor.app.data.repository.DeviceInfoRepository
import com.phonedoctor.app.data.repository.HistoryRepository
import com.phonedoctor.app.data.repository.MemoryRepository
import com.phonedoctor.app.data.repository.ScanEngine
import com.phonedoctor.app.data.repository.SensorsRepository
import com.phonedoctor.app.data.repository.StorageRepository

/**
 * Lightweight manual service locator used instead of a DI framework. The app
 * has a single process-wide dependency graph, so a small set of lazily
 * created singletons keeps things simple without Hilt/Dagger boilerplate.
 */
class ServiceLocator(context: Context) {

    private val appContext = context.applicationContext

    val database: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "phone_doctor.db").build()
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val batteryRepository: BatteryRepository by lazy { BatteryRepository(appContext) }
    val storageRepository: StorageRepository by lazy { StorageRepository(appContext) }
    val memoryRepository: MemoryRepository by lazy { MemoryRepository(appContext) }
    val deviceInfoRepository: DeviceInfoRepository by lazy { DeviceInfoRepository(appContext) }
    val sensorsRepository: SensorsRepository by lazy { SensorsRepository(appContext) }
    val connectivityRepository: ConnectivityRepository by lazy { ConnectivityRepository(appContext) }
    val historyRepository: HistoryRepository by lazy { HistoryRepository(database.scanHistoryDao()) }

    val scanEngine: ScanEngine by lazy {
        ScanEngine(
            batteryRepository = batteryRepository,
            storageRepository = storageRepository,
            memoryRepository = memoryRepository,
            sensorsRepository = sensorsRepository,
            connectivityRepository = connectivityRepository
        )
    }

    companion object {
        @Volatile private var instance: ServiceLocator? = null

        fun get(context: Context): ServiceLocator =
            instance ?: synchronized(this) {
                instance ?: ServiceLocator(context).also { instance = it }
            }
    }
}
