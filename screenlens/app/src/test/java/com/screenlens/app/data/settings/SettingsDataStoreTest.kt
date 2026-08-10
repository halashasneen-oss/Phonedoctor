package com.screenlens.app.data.settings

import androidx.test.core.app.ApplicationProvider
import com.screenlens.app.domain.AppLanguage
import com.screenlens.app.domain.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * JUnit doesn't guarantee method execution order, and DataStore's backing file
 * persists across @Test methods in this Robolectric setup, so each test must
 * start from a clean file rather than relying on being run first.
 */
@RunWith(RobolectricTestRunner::class)
class SettingsDataStoreTest {

    private val settingsDataStore = SettingsDataStore(ApplicationProvider.getApplicationContext())

    @Before
    fun clearPersistedSettings() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        File(context.filesDir, "datastore/screenlens_settings.preferences_pb").delete()
    }

    @Test
    fun `defaults are sensible before anything is written`() = runTest {
        val settings = settingsDataStore.settings.first()

        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(AppLanguage.SYSTEM, settings.appLanguage)
        assertFalse(settings.floatingLensEnabled)
        assertTrue(settings.autoSaveHistory)
        assertFalse(settings.onboardingCompleted)
    }

    @Test
    fun `setThemeMode persists and is reflected in the next read`() = runTest {
        settingsDataStore.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, settingsDataStore.settings.first().themeMode)
    }

    @Test
    fun `setAppLanguage persists Arabic`() = runTest {
        settingsDataStore.setAppLanguage(AppLanguage.ARABIC)

        assertEquals(AppLanguage.ARABIC, settingsDataStore.settings.first().appLanguage)
    }

    @Test
    fun `setHistoryLimit of zero means unlimited`() = runTest {
        settingsDataStore.setHistoryLimit(0)

        assertEquals(0, settingsDataStore.settings.first().historyLimit)
    }

    @Test
    fun `setOnboardingCompleted persists true`() = runTest {
        settingsDataStore.setOnboardingCompleted(true)

        assertTrue(settingsDataStore.settings.first().onboardingCompleted)
    }
}
