import Foundation

/// Plugin errors with stable codes and safe messages.
/// Messages never contain secret values, keys' contents or Keychain item data.
enum SecureStorageError: Error, Equatable {
    case invalidKey(String)
    case invalidValue(String)
    case keychain(OSStatus)

    var code: String {
        switch self {
        case .invalidKey: return "INVALID_KEY"
        case .invalidValue: return "INVALID_VALUE"
        case .keychain: return "KEYCHAIN_ERROR"
        }
    }

    var message: String {
        switch self {
        case .invalidKey(let reason), .invalidValue(let reason):
            return reason
        case .keychain(let status):
            // Only the numeric OSStatus is exposed; it carries no sensitive data.
            return "Keychain operation failed (OSStatus \(status))"
        }
    }
}
