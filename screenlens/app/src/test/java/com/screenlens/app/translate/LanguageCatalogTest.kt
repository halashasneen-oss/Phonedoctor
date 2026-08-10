package com.screenlens.app.translate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric provides the Android runtime that com.google.mlkit:translate's
// TranslateLanguage constants class expects, even though this test never touches UI.
@RunWith(RobolectricTestRunner::class)
class LanguageCatalogTest {

    @Test
    fun `catalog includes English and Arabic with correct native names`() {
        val english = LanguageCatalog.byCode("en")
        val arabic = LanguageCatalog.byCode("ar")

        assertNotNull(english)
        assertEquals("English", english?.englishName)
        assertNotNull(arabic)
        assertEquals("العربية", arabic?.nativeName)
    }

    @Test
    fun `unknown code returns null instead of a fake entry`() {
        assertNull(LanguageCatalog.byCode("not-a-real-code"))
    }

    @Test
    fun `catalog is sorted by English name`() {
        val names = LanguageCatalog.all.map { it.englishName }
        assertEquals(names.sorted(), names)
    }

    @Test
    fun `catalog has no duplicate codes`() {
        val codes = LanguageCatalog.all.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun `catalog is not empty`() {
        assertTrue(LanguageCatalog.all.isNotEmpty())
    }
}
