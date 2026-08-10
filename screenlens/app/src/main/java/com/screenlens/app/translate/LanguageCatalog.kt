package com.screenlens.app.translate

import com.google.mlkit.nl.translate.TranslateLanguage

/** A translatable language as exposed in ScreenLens UI (Translator, Language Manager). */
data class TranslateLanguageInfo(
    val code: String,
    val englishName: String,
    val nativeName: String
)

/**
 * The set of languages ML Kit's on-device Translation actually supports, with
 * display names. Built from TranslateLanguage so the list always matches what the
 * installed ML Kit version can really translate — nothing here is asserted that
 * the engine can't back up.
 */
object LanguageCatalog {

    private val displayNames: Map<String, Pair<String, String>> = mapOf(
        "af" to ("Afrikaans" to "Afrikaans"),
        "ar" to ("Arabic" to "العربية"),
        "be" to ("Belarusian" to "Беларуская"),
        "bg" to ("Bulgarian" to "Български"),
        "bn" to ("Bengali" to "বাংলা"),
        "ca" to ("Catalan" to "Català"),
        "cs" to ("Czech" to "Čeština"),
        "cy" to ("Welsh" to "Cymraeg"),
        "da" to ("Danish" to "Dansk"),
        "de" to ("German" to "Deutsch"),
        "el" to ("Greek" to "Ελληνικά"),
        "en" to ("English" to "English"),
        "eo" to ("Esperanto" to "Esperanto"),
        "es" to ("Spanish" to "Español"),
        "et" to ("Estonian" to "Eesti"),
        "fa" to ("Persian" to "فارسی"),
        "fi" to ("Finnish" to "Suomi"),
        "fr" to ("French" to "Français"),
        "ga" to ("Irish" to "Gaeilge"),
        "gl" to ("Galician" to "Galego"),
        "gu" to ("Gujarati" to "ગુજરાતી"),
        "he" to ("Hebrew" to "עברית"),
        "hi" to ("Hindi" to "हिन्दी"),
        "hr" to ("Croatian" to "Hrvatski"),
        "ht" to ("Haitian Creole" to "Kreyòl Ayisyen"),
        "hu" to ("Hungarian" to "Magyar"),
        "id" to ("Indonesian" to "Indonesia"),
        "is" to ("Icelandic" to "Íslenska"),
        "it" to ("Italian" to "Italiano"),
        "ja" to ("Japanese" to "日本語"),
        "ka" to ("Georgian" to "ქართული"),
        "kn" to ("Kannada" to "ಕನ್ನಡ"),
        "ko" to ("Korean" to "한국어"),
        "lt" to ("Lithuanian" to "Lietuvių"),
        "lv" to ("Latvian" to "Latviešu"),
        "mk" to ("Macedonian" to "Македонски"),
        "mr" to ("Marathi" to "मराठी"),
        "ms" to ("Malay" to "Melayu"),
        "mt" to ("Maltese" to "Malti"),
        "nl" to ("Dutch" to "Nederlands"),
        "no" to ("Norwegian" to "Norsk"),
        "pl" to ("Polish" to "Polski"),
        "pt" to ("Portuguese" to "Português"),
        "ro" to ("Romanian" to "Română"),
        "ru" to ("Russian" to "Русский"),
        "sk" to ("Slovak" to "Slovenčina"),
        "sl" to ("Slovenian" to "Slovenščina"),
        "sq" to ("Albanian" to "Shqip"),
        "sv" to ("Swedish" to "Svenska"),
        "sw" to ("Swahili" to "Kiswahili"),
        "ta" to ("Tamil" to "தமிழ்"),
        "te" to ("Telugu" to "తెలుగు"),
        "th" to ("Thai" to "ไทย"),
        "tl" to ("Tagalog" to "Tagalog"),
        "tr" to ("Turkish" to "Türkçe"),
        "uk" to ("Ukrainian" to "Українська"),
        "ur" to ("Urdu" to "اردو"),
        "vi" to ("Vietnamese" to "Tiếng Việt"),
        "zh" to ("Chinese" to "中文")
    )

    /** All languages the installed ML Kit Translate model set can translate, sorted by English name. */
    val all: List<TranslateLanguageInfo> by lazy {
        TranslateLanguage.getAllLanguages().mapNotNull { code ->
            val names = displayNames[code] ?: return@mapNotNull null
            TranslateLanguageInfo(code, names.first, names.second)
        }.sortedBy { it.englishName }
    }

    fun byCode(code: String): TranslateLanguageInfo? = all.find { it.code == code }

    fun toTranslateLanguageTag(code: String): String? = TranslateLanguage.fromLanguageTag(code)
}
