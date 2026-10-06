import Capacitor
import Foundation

@objc(SecureStoragePlugin)
public class SecureStoragePlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "SecureStoragePlugin"
    public let jsName = "SecureStorage"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "set", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "get", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "has", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "remove", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "clear", returnType: CAPPluginReturnPromise)
    ]

    private let store = KeychainStore()
    // Keychain calls are thread-safe; a concurrent queue keeps them off the main thread
    // without serializing unrelated operations.
    private let queue = DispatchQueue(label: "capacitor-secure-storage", qos: .userInitiated, attributes: .concurrent)

    @objc func set(_ call: CAPPluginCall) {
        run(call) { [store] in
            let key = try KeychainStore.validate(key: call.getString("key"))
            guard let value = call.getString("value") else {
                throw SecureStorageError.invalidValue("Value must be a string")
            }
            try store.set(value, for: key)
            call.resolve()
        }
    }

    @objc func get(_ call: CAPPluginCall) {
        run(call) { [store] in
            let key = try KeychainStore.validate(key: call.getString("key"))
            let value = try store.get(key)
            call.resolve(["value": value ?? NSNull()])
        }
    }

    @objc func has(_ call: CAPPluginCall) {
        run(call) { [store] in
            let key = try KeychainStore.validate(key: call.getString("key"))
            call.resolve(["value": try store.has(key)])
        }
    }

    @objc func remove(_ call: CAPPluginCall) {
        run(call) { [store] in
            let key = try KeychainStore.validate(key: call.getString("key"))
            try store.remove(key)
            call.resolve()
        }
    }

    @objc func clear(_ call: CAPPluginCall) {
        run(call) { [store] in
            try store.clear()
            call.resolve()
        }
    }

    private func run(_ call: CAPPluginCall, _ work: @escaping () throws -> Void) {
        queue.async {
            do {
                try work()
            } catch let error as SecureStorageError {
                call.reject(error.message, error.code)
            } catch {
                call.reject("Unexpected storage error", "STORAGE_ERROR")
            }
        }
    }
}
