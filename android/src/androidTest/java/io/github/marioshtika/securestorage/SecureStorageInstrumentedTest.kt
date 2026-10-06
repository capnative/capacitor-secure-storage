package io.github.marioshtika.securestorage

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureStorageInstrumentedTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefsName = "capacitor_secure_storage_test"
    private val alias = "capacitor-secure-storage-test-key"
    private lateinit var storage: SecureStorage

    @Before
    fun setUp() {
        storage = SecureStorage(context, prefsName, alias)
    }

    @After
    fun tearDown() {
        storage.clear()
        KeystoreCipher(alias).deleteKey()
    }

    private fun prefs() = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    @Test
    fun keyIsGeneratedInKeystore() {
        val cipher = KeystoreCipher(alias)
        cipher.encrypt("x".toByteArray(), "aad".toByteArray())
        assertTrue(cipher.hasKey())
    }

    @Test
    fun setGetHasRemove() {
        storage.set("access_token", "secret-value")
        assertEquals("secret-value", storage.get("access_token"))
        assertTrue(storage.has("access_token"))
        storage.remove("access_token")
        assertNull(storage.get("access_token"))
        assertFalse(storage.has("access_token"))
        storage.remove("access_token") // idempotent
    }

    @Test
    fun overwriteAndMissing() {
        assertNull(storage.get("missing"))
        storage.set("k", "one")
        storage.set("k", "two")
        assertEquals("two", storage.get("k"))
    }

    @Test
    fun unicodeEmptyAndLongValues() {
        storage.set("ключ-🔑", "значение 🙂 日本語 \u0000 end")
        assertEquals("значение 🙂 日本語 \u0000 end", storage.get("ключ-🔑"))
        storage.set("empty", "")
        assertEquals("", storage.get("empty"))
        val long = "a".repeat(1_000_000)
        storage.set("long", long)
        assertEquals(long, storage.get("long"))
    }

    @Test
    fun clearRemovesEverything() {
        storage.set("a", "1")
        storage.set("b", "2")
        storage.clear()
        assertFalse(storage.has("a"))
        assertFalse(storage.has("b"))
    }

    @Test
    fun persistsAcrossInstances() {
        storage.set("k", "v")
        assertEquals("v", SecureStorage(context, prefsName, alias).get("k"))
    }

    @Test
    fun plaintextAndKeyNamesAreNeverPersisted() {
        storage.set("access_token", "super-secret-plaintext")
        val dump = prefs().all.entries.joinToString { "${it.key}=${it.value}" }
        assertFalse(dump.contains("super-secret-plaintext"))
        assertFalse(dump.contains("access_token"))
    }

    @Test
    fun everySetUsesANewIv() {
        storage.set("k", "same")
        val first = EncryptedPayload.decode(prefs().getString(SecureStorage.storageKey("k"), null)!!)!!
        storage.set("k", "same")
        val second = EncryptedPayload.decode(prefs().getString(SecureStorage.storageKey("k"), null)!!)!!
        assertNotEquals(first.iv.toList(), second.iv.toList())
        assertNotEquals(first.cipherText.toList(), second.cipherText.toList())
    }

    @Test
    fun corruptedCiphertextFailsToDecrypt() {
        storage.set("k", "value")
        val sk = SecureStorage.storageKey("k")
        val payload = EncryptedPayload.decode(prefs().getString(sk, null)!!)!!
        payload.cipherText[0] = (payload.cipherText[0].toInt() xor 1).toByte()
        prefs().edit().putString(sk, payload.encode()).commit()
        val e = assertThrows(SecureStorageException::class.java) { storage.get("k") }
        assertEquals(SecureStorageException.DECRYPTION_ERROR, e.code)
    }

    @Test
    fun malformedStoredValueFailsSafely() {
        prefs().edit().putString(SecureStorage.storageKey("k"), "garbage").commit()
        val e = assertThrows(SecureStorageException::class.java) { storage.get("k") }
        assertEquals(SecureStorageException.DECRYPTION_ERROR, e.code)
    }

    @Test
    fun ciphertextCannotBeMovedBetweenKeys() {
        storage.set("a", "value-a")
        val moved = prefs().getString(SecureStorage.storageKey("a"), null)
        prefs().edit().putString(SecureStorage.storageKey("b"), moved).commit()
        assertThrows(SecureStorageException::class.java) { storage.get("b") }
    }

    @Test
    fun deletedKeyMakesValuesUndecryptable() {
        storage.set("k", "v")
        KeystoreCipher(alias).deleteKey()
        val e = assertThrows(SecureStorageException::class.java) { storage.get("k") }
        assertEquals(SecureStorageException.DECRYPTION_ERROR, e.code)
    }
}
