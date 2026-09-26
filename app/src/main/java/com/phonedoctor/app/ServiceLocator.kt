package com.phonedoctor.app

import android.content.Context
import androidx.room.Room
import com.phonedoctor.app.data.datastore.SettingsRepository
import com.phonedoctor.app.data.local.AppDatabase
import com.phonedoctor.app.data.repository.AudioDiagnosticsRepository
import com.phonedoctor.app.data.repository.BatteryRepository
import com.phonedoctor.app.data.repository.CameraDiagnosticsRepository
import com.phonedoctor.app.data.repository.ConnectivityRepository
import com.phonedoctor.app.data.repository.CpuBenchmarkEngine
import com.phonedoctor.app.data.repository.CpuRepository
import com.phonedoctor.app.data.repository.DeviceInfoRepository
import com.phonedoctor.app.data.repository.GpuMediaRepository
import com.phonedoctor.app.data.repository.HistoryRepository
import com.phonedoctor.app.data.repository.MemoryBenchmarkEngine
import com.phonedoctor.app.data.repository.MemoryRepository
import com.phonedoctor.app.data.repository.NetworkDiagnosticsEngine
import com.phonedoctor.app.data.repository.ScanEngine
import com.phonedoctor.app.data.repository.SensorsRepository
import com.phonedoctor.app.data.repository.StorageBenchmarkEngine
import com.phonedoctor.app.data.repository.StorageRepository
import com.phonedoctor.app.data.repository.ThermalRepository

/**
 * Lightweight manual service locator used instead of a DI framework. The app
 * has a single process-wide dependency graph, so a small set of lazily
 * created singletons keeps things simple without Hilt/Dagger boilerplate.
 */
class ServiceLocator(context: Context) {

    private val appContext = context.applicationContext

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "phone_doctor.db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val batteryRepository: BatteryRepository by lazy { BatteryRepository(appContext) }
    val cameraDiagnosticsRepository: CameraDiagnosticsRepository by lazy { CameraDiagnosticsRepository(appContext) }
    val audioDiagnosticsRepository: AudioDiagnosticsRepository by lazy { AudioDiagnosticsRepository(appContext) }
    val storageRepository: StorageRepository by lazy { StorageRepository(appContext) }
    val storageBenchmarkEngine: StorageBenchmarkEngine by lazy { StorageBenchmarkEngine(appContext) }
    val memoryRepository: MemoryRepository by lazy { MemoryRepository(appContext) }
    val memoryBenchmarkEngine: MemoryBenchmarkEngine by lazy { MemoryBenchmarkEngine() }
    val deviceInfoRepository: DeviceInfoRepository by lazy { DeviceInfoRepository(appContext) }
    val sensorsRepository: SensorsRepository by lazy { SensorsRepository(appContext) }
    val connectivityRepository: ConnectivityRepository by lazy { ConnectivityRepository(appContext) }
    val networkDiagnosticsEngine: NetworkDiagnosticsEngine by lazy { NetworkDiagnosticsEngine() }
    val cpuRepository: CpuRepository by lazy { CpuRepository() }
    val cpuBenchmarkEngine: CpuBenchmarkEngine by lazy { CpuBenchmarkEngine() }
    val thermalRepository: ThermalRepository by lazy { ThermalRepository(appContext) }
    val historyRepository: HistoryRepository by lazy { HistoryRepository(database.scanHistoryDao()) }
    val gpuMediaRepository: GpuMediaRepository by lazy { GpuMediaRepository(appContext) }

    val scanEngine: ScanEngine by lazy {
        ScanEngine(
            batteryRepository = batteryRepository,
            storageRepository = storageRepository,
            memoryRepository = memoryRepository,
            thermalRepository = thermalRepository,
            sensorsRepository = sensorsRepository,
            connectivityRepository = connectivityRepository,
            cpuBenchmarkEngine = cpuBenchmarkEngine,
            memoryBenchmarkEngine = memoryBenchmarkEngine,
            storageBenchmarkEngine = storageBenchmarkEngine
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
