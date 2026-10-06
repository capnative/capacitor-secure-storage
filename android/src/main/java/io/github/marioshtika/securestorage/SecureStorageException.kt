package io.github.marioshtika.securestorage

/**
 * A storage failure with a stable error code and a safe message.
 * Messages must never contain secrets, key material, ciphertext or decrypted data,
 * and the underlying cause is intentionally never forwarded to JavaScript.
 */
class SecureStorageException(
    val code: String,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    companion object {
        const val INVALID_KEY = "INVALID_KEY"
        const val INVALID_VALUE = "INVALID_VALUE"
        const val STORAGE_ERROR = "STORAGE_ERROR"
        const val KEYSTORE_ERROR = "KEYSTORE_ERROR"
        const val ENCRYPTION_ERROR = "ENCRYPTION_ERROR"
        const val DECRYPTION_ERROR = "DECRYPTION_ERROR"
    }
}
