package io.github.marioshtika.securestorage

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

/**
 * Persists values encrypted by [KeystoreCipher] in a dedicated SharedPreferences file.
 * The file is only a persistence layer: it contains hashed key names and ciphertext, never plaintext values.
 * The key name is bound to the ciphertext as GCM associated data, so entries cannot be swapped between keys.
 */
class SecureStorage(
    private val prefs: SharedPreferences,
    private val cipher: KeystoreCipher = KeystoreCipher(),
) {

    constructor(context: Context, prefsName: String = PREFS_NAME, alias: String = KeystoreCipher.DEFAULT_ALIAS) :
        this(
            context.applicationContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE),
            KeystoreCipher(alias),
        )

    fun set(key: String, value: String) {
        val storageKey = storageKey(key)
        val payload = cipher.encrypt(value.toByteArray(Charsets.UTF_8), storageKey.toByteArray(Charsets.UTF_8))
        // commit() is synchronous so failures are reported; we are never on the UI thread here.
        if (!prefs.edit().putString(storageKey, payload.encode()).commit()) {
            throw SecureStorageException(SecureStorageException.STORAGE_ERROR, "Failed to persist value")
        }
    }

    /** Returns the decrypted value, or null if the key doesn't exist. Throws if decryption fails. */
    fun get(key: String): String? {
        val storageKey = storageKey(key)
        val encoded = prefs.getString(storageKey, null) ?: return null
        val payload = EncryptedPayload.decode(encoded)
            ?: throw SecureStorageException(SecureStorageException.DECRYPTION_ERROR, "Stored value is corrupted")
        val bytes = cipher.decrypt(payload, storageKey.toByteArray(Charsets.UTF_8))
        return String(bytes, Charsets.UTF_8)
    }

    fun has(key: String): Boolean = prefs.contains(storageKey(key))

    fun remove(key: String) {
        if (!prefs.edit().remove(storageKey(key)).commit()) {
            throw SecureStorageException(SecureStorageException.STORAGE_ERROR, "Failed to remove value")
        }
    }

    /** Removes only entries of this plugin's dedicated preferences file. The Keystore key is kept. */
    fun clear() {
        if (!prefs.edit().clear().commit()) {
            throw SecureStorageException(SecureStorageException.STORAGE_ERROR, "Failed to clear values")
        }
    }

    companion object {
        const val PREFS_NAME = "capacitor_secure_storage"
        const val MAX_KEY_BYTES = 256

        /** Throws INVALID_KEY unless [key] is a non-empty string of at most [MAX_KEY_BYTES] UTF-8 bytes. */
        fun validateKey(key: String?): String {
            if (key.isNullOrEmpty()) {
                throw SecureStorageException(SecureStorageException.INVALID_KEY, "Key must be a non-empty string")
            }
            if (key.toByteArray(Charsets.UTF_8).size > MAX_KEY_BYTES) {
                throw SecureStorageException(
                    SecureStorageException.INVALID_KEY,
                    "Key must be at most $MAX_KEY_BYTES bytes",
                )
            }
            return key
        }

        /** Key names are hashed so that they are not stored in plaintext either. */
        fun storageKey(key: String): String =
            MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
