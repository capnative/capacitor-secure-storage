# Capacitor Secure Storage

A Capacitor plugin for storing string key-value data securely using the **iOS Keychain** and the **Android Keystore** (AES-256-GCM).

## Compatibility

| Plugin version | Capacitor compatibility | Maintained |
| -------------- | ----------------------- | ---------- |
| v8.\*.\*       | v8.\*.\*                | ✅         |

## Supported platforms

| Platform | Support | Minimum version            |
| -------- | ------- | -------------------------- |
| iOS      | ✅      | iOS 15                     |
| Android  | ✅      | API level 24 (Android 7.0) |
| Web      | ❌      | —                          |

## Install

```bash
npm install capacitor-secure-storage
npx cap sync
```

## Basic usage

```typescript
import { SecureStorage } from 'capacitor-secure-storage';

await SecureStorage.set({ 
    key: 'access_token', 
    value: 'my-secret-token'
});

const { value } = await SecureStorage.get({ 
    key: 'access_token'
});
```

## API

<docgen-index>

* [`set(...)`](#set)
* [`get(...)`](#get)
* [`has(...)`](#has)
* [`remove(...)`](#remove)
* [`clear()`](#clear)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### set(...)

```typescript
set(options: { key: string; value: string; }) => Promise<void>
```

Stores a string value securely. If the key already exists, its value is overwritten. Empty string values are allowed.

**Options:**

| Param | Type |
| --- | --- |
| **`options`** | <code>{ key: string; value: string; }</code> |

--------------------


### get(...)

```typescript
get(options: { key: string; }) => Promise<{ value: string | null; }>
```

Retrieves a stored value. The value is `null` if the key does not exist.

**Options:**

| Param | Type |
| --- | --- |
| **`options`** | <code>{ key: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string | null; }&gt;</code>

--------------------


### has(...)

```typescript
has(options: { key: string; }) => Promise<{ value: boolean; }>
```

Checks whether a key exists.

**Options:**

| Param | Type |
| --- | --- |
| **`options`** | <code>{ key: string; }</code> |

**Returns:** <code>Promise&lt;{ value: boolean; }&gt;</code>

--------------------


### remove(...)

```typescript
remove(options: { key: string; }) => Promise<void>
```

Deletes a key. Succeeds even if the key does not exist.

**Options:**

| Param | Type |
| --- | --- |
| **`options`** | <code>{ key: string; }</code> |

--------------------


### clear()

```typescript
clear() => Promise<void>
```

Deletes every value stored by this plugin and nothing else.

--------------------

</docgen-api>

Keys must be non-empty and at most 256 UTF-8 bytes. Values must be strings.

## Errors

If an operation fails, its promise rejects with a stable `code` and a safe message. Handle errors using `code`, not the message text.

| Code | Platform | Meaning |
| ---- | -------- | ------- |
| `INVALID_KEY` | iOS, Android | The key is empty or exceeds 256 UTF-8 bytes. |
| `INVALID_VALUE` | iOS, Android | The value passed to `set` is not a string. |
| `STORAGE_ERROR` | iOS, Android | An unexpected storage operation failed, or Android could not persist a change. |
| `KEYCHAIN_ERROR` | iOS | The iOS Keychain operation failed. |
| `KEYSTORE_ERROR` | Android | The Android Keystore could not be accessed or updated. |
| `ENCRYPTION_ERROR` | Android | The value could not be encrypted. |
| `DECRYPTION_ERROR` | Android | The stored value is corrupt, tampered with, or cannot be decrypted with the available Keystore key. |
| `UNAVAILABLE` | Web | Secure storage is not supported on the web; there is no insecure storage fallback. |
