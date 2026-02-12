package io.celox.stupidisco.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class ApiKeyStore(context: Context) {
    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            "stupidisco_keys",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getDeepgramKey(): String = prefs.getString(KEY_DEEPGRAM, "") ?: ""

    fun getAnthropicKey(): String = prefs.getString(KEY_ANTHROPIC, "") ?: ""

    fun saveKeys(deepgramKey: String, anthropicKey: String) {
        prefs.edit()
            .putString(KEY_DEEPGRAM, deepgramKey)
            .putString(KEY_ANTHROPIC, anthropicKey)
            .apply()
    }

    fun hasKeys(): Boolean {
        return getDeepgramKey().isNotBlank() && getAnthropicKey().isNotBlank()
    }

    companion object {
        private const val KEY_DEEPGRAM = "deepgram_api_key"
        private const val KEY_ANTHROPIC = "anthropic_api_key"
    }
}
