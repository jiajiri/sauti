package com.sauti.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Enterprise Encrypted Storage Engine.
 * Encrypts all key-value preferences, session tokens, user identity, and metadata at rest
 * using hardware-backed MasterKey (Android Keystore) with AES-256-SIV and AES-256-GCM.
 */
class EncryptedStorageManager private constructor(context: Context) {

    private val sharedPreferences: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPreferences = EncryptedSharedPreferences.create(
            context.applicationContext,
            ENCRYPTED_PREFS_FILENAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun putString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun putLong(key: String, value: Long) {
        sharedPreferences.edit().putLong(key, value).apply()
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }

    fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        private const val ENCRYPTED_PREFS_FILENAME = "sauti_encrypted_vault_prefs"

        @Volatile
        private var INSTANCE: EncryptedStorageManager? = null

        fun getInstance(context: Context): EncryptedStorageManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EncryptedStorageManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
