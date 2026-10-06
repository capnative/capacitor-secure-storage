package io.github.marioshtika.securestorage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SecureStorageValidationTest {

    @Test
    fun rejectsEmptyAndNullKeys() {
        assertEquals("INVALID_KEY", assertThrows(SecureStorageException::class.java) { SecureStorage.validateKey("") }.code)
        assertEquals("INVALID_KEY", assertThrows(SecureStorageException::class.java) { SecureStorage.validateKey(null) }.code)
    }

    @Test
    fun rejectsOverlongKeys() {
        assertThrows(SecureStorageException::class.java) { SecureStorage.validateKey("a".repeat(257)) }
    }

    @Test
    fun acceptsUnicodeKeys() {
        assertEquals("ключ-🔑", SecureStorage.validateKey("ключ-🔑"))
    }

    @Test
    fun storageKeyIsHashedAndStable() {
        val a = SecureStorage.storageKey("access_token")
        assertEquals(a, SecureStorage.storageKey("access_token"))
        assertNotEquals("access_token", a)
        assertEquals(64, a.length)
    }
}
