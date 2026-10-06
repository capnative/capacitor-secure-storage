# capacitor-secure-storage

A Capacitor plugin for storing string key-value data securely using the **iOS Keychain** and the **Android Keystore** (AES-256-GCM). The JavaScript API is identical on both platforms.

## Installation

```bash
npm install capacitor-secure-storage
npx cap sync
```

Requires Capacitor 8 (iOS 15+, Android minSdk 24, Swift Package Manager or CocoaPods, Kotlin).

## Usage

```ts
import { SecureStorage } from 'capacitor-secure-storage';

await SecureStorage.set({ key: 'access_token', value: 'my-secret-token' });

const { value } = await SecureStorage.get({ key: 'access_token' }); // string | null

const { value: exists } = await SecureStorage.has({ key: 'access_token' });

await SecureStorage.remove({ key: 'access_token' });
```

## API

| Method | Description |
| --- | --- |
| `set({ key, value })` | Stores `value`, overwriting any existing value. Empty string values are allowed. |
| `get({ key })` | Resolves `{ value }`; `value` is `null` if the key does not exist. |
| `has({ key })` | Resolves `{ value: boolean }`. |
| `remove({ key })` | Deletes the key. Succeeds if the key does not exist. |
| `clear()` | Deletes every value stored by this plugin (and nothing else). |

Keys must be non-empty and at most 256 UTF-8 bytes. Values must be strings.

### Errors

Rejections carry a `code` and a safe message (never secrets, key material or ciphertext):

`INVALID_KEY`, `INVALID_VALUE`, `STORAGE_ERROR`, `KEYCHAIN_ERROR` (iOS), `KEYSTORE_ERROR`, `ENCRYPTION_ERROR`, `DECRYPTION_ERROR` (Android), `UNAVAILABLE` (web).

## Security

- **iOS**: values are generic-password Keychain items under the service `capacitor-secure-storage`, with `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` and `kSecAttrSynchronizable = false` (no iCloud Keychain sync). `clear()` deletes only items of that service. UserDefaults is never used.
- **Android**: a non-exportable AES-256 key (alias `capacitor-secure-storage-key`) lives in the Android Keystore. Values are encrypted with AES/GCM (fresh random 96-bit IV per `set()`) and persisted as `v1:base64(iv):base64(ciphertext+tag)` in a dedicated SharedPreferences file, which is only a persistence layer. Key names are stored as SHA-256 hashes and are bound to the ciphertext as GCM associated data, so entries cannot be swapped between keys. Tampered or undecryptable data causes a `DECRYPTION_ERROR`; plaintext is never returned from corrupt data.
- **Web**: **not supported and not secure**. All methods reject with `UNAVAILABLE`; there is intentionally no fallback to `localStorage`.
- The plugin never logs values, keys or key material. Native work runs off the UI thread; concurrent calls are safe (only Android key creation is locked).
- No biometric authentication in this version; the design leaves room for a future `requireBiometrics` option.

### Limitations

- Keystore/Keychain protection does not defend against a rooted/jailbroken device or a compromised OS, nor against code running inside your app process.
- Android Keystore keys may be hardware-backed (TEE/StrongBox) depending on the device; this plugin does not enforce StrongBox.
- Keychain data on iOS and the Keystore key on Android are not erased in the same way by every OS version; see below.
- Android's `has` only checks that an entry exists; an undecryptable entry reports `true` while `get` rejects.
- Android's `get` for a corrupted entry fails rather than returning `null`; call `remove` to discard it.

### Backup, restore, reinstall, device change

- **iOS backup/restore**: because items are `ThisDeviceOnly`, they are not included in iCloud backups or unencrypted-device migrations and are not restored to a different device. They are restored only for an encrypted backup restored to the same device.
- **iOS uninstall/reinstall**: Keychain items can survive app deletion and may still be present after reinstall. Call `clear()` on first launch if you need a clean slate.
- **Android backup/restore**: Keystore keys never leave the device, so restored ciphertext cannot be decrypted (`DECRYPTION_ERROR`). Exclude the `capacitor_secure_storage` SharedPreferences file from backups in your app (`android:fullBackupContent` / `android:dataExtractionRules`) and treat such errors as "value missing" (`remove` the entry and ask the user to sign in again).
- **Android uninstall / clear data**: both the preferences file and the Keystore key are removed.
- **Device change**: secrets are not migrated by design; re-authenticate and store them again.

## Development

```bash
npm install
npm run build        # compile TypeScript
npm test             # TypeScript tests (vitest)
npm run verify:ios   # swift build && swift test (macOS)
npm run verify:android  # needs Gradle and the Android SDK; instrumented tests: gradle connectedAndroidTest (in android/)
```

Android instrumented tests (`android/src/androidTest`) verify encryption, unique IVs, tamper detection and that no plaintext is persisted. iOS tests (`ios/Tests`) verify Keychain storage, accessibility, service-scoped `clear()` and that UserDefaults is not used.
