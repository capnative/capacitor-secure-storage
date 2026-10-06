import Foundation
import Security

/// Thin wrapper around Keychain Services (generic password items).
///
/// Every item is scoped to a dedicated `kSecAttrService`, so `clear()` can never touch
/// unrelated Keychain entries. Items use `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`
/// and are never synchronized through iCloud Keychain. Values are never logged.
final class KeychainStore {
    static let defaultService = "capacitor-secure-storage"
    static let maxKeyBytes = 256

    private let service: String

    init(service: String = KeychainStore.defaultService) {
        self.service = service
    }

    static func validate(key: String?) throws -> String {
        guard let key = key, !key.isEmpty else {
            throw SecureStorageError.invalidKey("Key must be a non-empty string")
        }
        guard key.utf8.count <= maxKeyBytes else {
            throw SecureStorageError.invalidKey("Key must be at most \(maxKeyBytes) bytes")
        }
        return key
    }

    /// Base query identifying this plugin's items (optionally a single account).
    private func baseQuery(account: String? = nil) -> [String: Any] {
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            // Match any sync state so remove()/clear() also catch items synced by older versions.
            kSecAttrSynchronizable as String: kSecAttrSynchronizableAny
        ]
        if let account = account {
            query[kSecAttrAccount as String] = account
        }
        return query
    }

    func set(_ value: String, for key: String) throws {
        let data = Data(value.utf8)
        let updateStatus = SecItemUpdate(
            baseQuery(account: key) as CFDictionary,
            [kSecValueData as String: data] as CFDictionary
        )
        switch updateStatus {
        case errSecSuccess:
            return
        case errSecItemNotFound:
            var attributes = baseQuery(account: key)
            attributes[kSecValueData as String] = data
            attributes[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
            // Replace the "any" match with an explicit non-synchronizable attribute.
            attributes[kSecAttrSynchronizable as String] = kCFBooleanFalse
            let addStatus = SecItemAdd(attributes as CFDictionary, nil)
            if addStatus == errSecDuplicateItem {
                // Lost a race with a concurrent set(); the item now exists, so update it.
                let retry = SecItemUpdate(
                    baseQuery(account: key) as CFDictionary,
                    [kSecValueData as String: data] as CFDictionary
                )
                guard retry == errSecSuccess else { throw SecureStorageError.keychain(retry) }
            } else if addStatus != errSecSuccess {
                throw SecureStorageError.keychain(addStatus)
            }
        default:
            throw SecureStorageError.keychain(updateStatus)
        }
    }

    /// Returns nil if the key does not exist.
    func get(_ key: String) throws -> String? {
        var query = baseQuery(account: key)
        query[kSecReturnData as String] = kCFBooleanTrue
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        switch status {
        case errSecSuccess:
            guard let data = result as? Data, let string = String(data: data, encoding: .utf8) else {
                throw SecureStorageError.invalidValue("Stored value is not valid UTF-8")
            }
            return string
        case errSecItemNotFound:
            return nil
        default:
            throw SecureStorageError.keychain(status)
        }
    }

    func has(_ key: String) throws -> Bool {
        var query = baseQuery(account: key)
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        let status = SecItemCopyMatching(query as CFDictionary, nil)
        switch status {
        case errSecSuccess: return true
        case errSecItemNotFound: return false
        default: throw SecureStorageError.keychain(status)
        }
    }

    /// A missing key is not an error.
    func remove(_ key: String) throws {
        let status = SecItemDelete(baseQuery(account: key) as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw SecureStorageError.keychain(status)
        }
    }

    /// Deletes only items belonging to this store's service.
    func clear() throws {
        let status = SecItemDelete(baseQuery() as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw SecureStorageError.keychain(status)
        }
    }
}
