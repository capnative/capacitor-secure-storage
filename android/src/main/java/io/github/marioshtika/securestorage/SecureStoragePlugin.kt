package io.github.marioshtika.securestorage

import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

@CapacitorPlugin(name = "SecureStorage")
class SecureStoragePlugin : Plugin() {

    private val storage: SecureStorage by lazy { SecureStorage(context) }

    // Plugin methods run on Capacitor's background plugin thread (not the UI thread).
    // SharedPreferences is thread-safe and only key creation is locked, so calls run concurrently.

    @PluginMethod
    fun set(call: PluginCall) = handle(call) {
        val key = SecureStorage.validateKey(call.getString("key"))
        val value = call.getString("value")
            ?: throw SecureStorageException(SecureStorageException.INVALID_VALUE, "Value must be a string")
        storage.set(key, value)
        call.resolve()
    }

    @PluginMethod
    fun get(call: PluginCall) = handle(call) {
        val key = SecureStorage.validateKey(call.getString("key"))
        val result = JSObject()
        result.put("value", storage.get(key) ?: JSObject.NULL)
        call.resolve(result)
    }

    @PluginMethod
    fun has(call: PluginCall) = handle(call) {
        val key = SecureStorage.validateKey(call.getString("key"))
        val result = JSObject()
        result.put("value", storage.has(key))
        call.resolve(result)
    }

    @PluginMethod
    fun remove(call: PluginCall) = handle(call) {
        storage.remove(SecureStorage.validateKey(call.getString("key")))
        call.resolve()
    }

    @PluginMethod
    fun clear(call: PluginCall) = handle(call) {
        storage.clear()
        call.resolve()
    }

    private fun handle(call: PluginCall, block: () -> Unit) {
        try {
            block()
        } catch (e: SecureStorageException) {
            // Only the safe message and code are exposed; the cause is never forwarded.
            call.reject(e.message, e.code)
        } catch (e: Exception) {
            call.reject("Unexpected storage error", SecureStorageException.STORAGE_ERROR)
        }
    }
}
