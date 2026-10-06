package io.github.marioshtika.securestorage

import android.util.Base64

/**
 * Persistent representation of an encrypted value: `version:base64(iv):base64(ciphertext+tag)`.
 * The GCM authentication tag is appended to the ciphertext by the JCA provider.
 */
class EncryptedPayload(val iv: ByteArray, val cipherText: ByteArray) {

    fun encode(): String =
        listOf(VERSION, b64(iv), b64(cipherText)).joinToString(SEPARATOR)

    companion object {
        const val VERSION = "v1"
        private const val SEPARATOR = ":"
        const val IV_LENGTH_BYTES = 12
        const val TAG_LENGTH_BYTES = 16

        private fun b64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)

        /** Parses a stored string; returns null if it is malformed. */
        fun decode(encoded: String): EncryptedPayload? {
            val parts = encoded.split(SEPARATOR)
            if (parts.size != 3 || parts[0] != VERSION) return null
            return try {
                val iv = Base64.decode(parts[1], Base64.NO_WRAP)
                val ct = Base64.decode(parts[2], Base64.NO_WRAP)
                if (iv.size != IV_LENGTH_BYTES || ct.size < TAG_LENGTH_BYTES) null else EncryptedPayload(iv, ct)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
