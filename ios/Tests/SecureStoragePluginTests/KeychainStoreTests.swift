import Security
import XCTest
@testable import SecureStoragePlugin

final class KeychainStoreTests: XCTestCase {
    private let service = "capacitor-secure-storage-tests"
    private var store: KeychainStore!

    override func setUpWithError() throws {
        store = KeychainStore(service: service)
        try store.clear()
    }

    override func tearDownWithError() throws {
        try store.clear()
    }

    func testSetGetHasRemove() throws {
        try store.set("secret-value", for: "access_token")
        XCTAssertEqual(try store.get("access_token"), "secret-value")
        XCTAssertTrue(try store.has("access_token"))
        try store.remove("access_token")
        XCTAssertNil(try store.get("access_token"))
        XCTAssertFalse(try store.has("access_token"))
        XCTAssertNoThrow(try store.remove("access_token"))
    }

    func testOverwrite() throws {
        try store.set("one", for: "k")
        try store.set("two", for: "k")
        XCTAssertEqual(try store.get("k"), "two")
    }

    func testMissingKey() throws {
        XCTAssertNil(try store.get("missing"))
    }

    func testUnicodeEmojiEmptyAndNullBytes() throws {
        try store.set("значение 🙂 日本語", for: "ключ-🔑")
        XCTAssertEqual(try store.get("ключ-🔑"), "значение 🙂 日本語")
        try store.set("", for: "empty")
        XCTAssertEqual(try store.get("empty"), "")
        try store.set("a\u{0}b", for: "nul")
        XCTAssertEqual(try store.get("nul"), "a\u{0}b")
    }

    func testPersistsAcrossInstances() throws {
        try store.set("v", for: "k")
        XCTAssertEqual(try KeychainStore(service: service).get("k"), "v")
    }

    func testClearOnlyAffectsOwnService() throws {
        let other = KeychainStore(service: service + ".other")
        try other.clear()
        try store.set("1", for: "a")
        try other.set("2", for: "a")
        try store.clear()
        XCTAssertNil(try store.get("a"))
        XCTAssertEqual(try other.get("a"), "2")
        try other.clear()
    }

    func testStoredInKeychainWithDeviceOnlyAccessibility() throws {
        try store.set("v", for: "k")
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: "k",
            kSecReturnAttributes as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var result: CFTypeRef?
        XCTAssertEqual(SecItemCopyMatching(query as CFDictionary, &result), errSecSuccess)
        let attributes = result as? [String: Any]
        XCTAssertEqual(attributes?[kSecAttrAccessible as String] as? String,
                       kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly as String)
    }

    func testDoesNotUseUserDefaults() throws {
        try store.set("plain-secret-check", for: "ud")
        let dump = UserDefaults.standard.dictionaryRepresentation().description
        XCTAssertFalse(dump.contains("plain-secret-check"))
    }

    func testKeyValidation() {
        XCTAssertThrowsError(try KeychainStore.validate(key: "")) {
            XCTAssertEqual(($0 as? SecureStorageError)?.code, "INVALID_KEY")
        }
        XCTAssertThrowsError(try KeychainStore.validate(key: nil))
        XCTAssertThrowsError(try KeychainStore.validate(key: String(repeating: "a", count: 257)))
        XCTAssertNoThrow(try KeychainStore.validate(key: "ключ-🔑"))
    }

    func testKeychainErrorMessageIsSafe() {
        let error = SecureStorageError.keychain(errSecAuthFailed)
        XCTAssertEqual(error.code, "KEYCHAIN_ERROR")
        XCTAssertFalse(error.message.isEmpty)
    }
}
