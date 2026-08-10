package com.screenlens.app.translate

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Wraps Google ML Kit's on-device Translation. Nothing is translated via a
 * ScreenLens server — translation runs fully offline once both language models
 * are downloaded. Model downloads go straight to Google's ML Kit model service
 * and are never routed through, or initiated by, a ScreenLens backend.
 */
class TranslationEngine(private val appContext: Context) {

    private val modelManager by lazy { RemoteModelManager.getInstance() }
    private val translators = ConcurrentHashMap<String, Translator>()

    private fun hasInternet(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun downloadedModelCodes(): Set<String> = withContext(Dispatchers.IO) {
        runCatching {
            modelManager.getDownloadedModels(TranslateRemoteModel::class.java).await()
                .map { it.language }
                .toSet()
        }.getOrDefault(emptySet())
    }

    suspend fun downloadModel(languageCode: String, requireWifi: Boolean): ModelDownloadOutcome {
        if (!hasInternet()) return ModelDownloadOutcome.NoInternet
        return withContext(Dispatchers.IO) {
            try {
                val model = TranslateRemoteModel.Builder(languageCode).build()
                val conditions = DownloadConditions.Builder()
                    .apply { if (requireWifi) requireWifi() }
                    .build()
                modelManager.download(model, conditions).await()
                ModelDownloadOutcome.Success
            } catch (t: Throwable) {
                ModelDownloadOutcome.Failed(t.message)
            }
        }
    }

    suspend fun deleteModel(languageCode: String): ModelDownloadOutcome = withContext(Dispatchers.IO) {
        try {
            val model = TranslateRemoteModel.Builder(languageCode).build()
            modelManager.deleteDownloadedModel(model).await()
            translators.keys.filter { it.startsWith("$languageCode:") || it.endsWith(":$languageCode") }
                .forEach { translators.remove(it)?.close() }
            ModelDownloadOutcome.Success
        } catch (t: Throwable) {
            ModelDownloadOutcome.Failed(t.message)
        }
    }

    suspend fun translate(text: String, sourceCode: String, targetCode: String): TranslationOutcome {
        if (text.isBlank()) return TranslationOutcome.Success("")
        val installed = downloadedModelCodes()
        val missing = listOf(sourceCode, targetCode).filter { it !in installed }
        if (missing.isNotEmpty()) return TranslationOutcome.ModelsNotInstalled(missing)

        return withContext(Dispatchers.Default) {
            try {
                val translator = translatorFor(sourceCode, targetCode)
                val result = translator.translate(text).await()
                TranslationOutcome.Success(result)
            } catch (t: Throwable) {
                TranslationOutcome.Failed(t.message)
            }
        }
    }

    private fun translatorFor(sourceCode: String, targetCode: String): Translator {
        val key = "$sourceCode:$targetCode"
        return translators.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceCode)
                .setTargetLanguage(targetCode)
                .build()
            Translation.getClient(options)
        }
    }
}
