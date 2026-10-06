/**
 * Error codes reported by the native implementations (as `error.code`).
 */
export type SecureStorageErrorCode =
  | 'INVALID_KEY'
  | 'INVALID_VALUE'
  | 'STORAGE_ERROR'
  | 'KEYCHAIN_ERROR'
  | 'KEYSTORE_ERROR'
  | 'ENCRYPTION_ERROR'
  | 'DECRYPTION_ERROR'
  | 'UNAVAILABLE';

/** Maximum key length (in UTF-8 bytes) accepted on every platform. */
export const MAX_KEY_LENGTH = 256;

export interface SecureStoragePlugin {
  /**
   * Store a string value securely.
   * If the key already exists, overwrite its value.
   */
  set(options: { key: string; value: string }): Promise<void>;

  /**
   * Retrieve a previously stored value.
   *
   * Returns null if the key does not exist.
   */
  get(options: { key: string }): Promise<{ value: string | null }>;

  /**
   * Check whether a key exists.
   */
  has(options: { key: string }): Promise<{ value: boolean }>;

  /**
   * Delete a key.
   *
   * Succeeds even if the key does not exist.
   */
  remove(options: { key: string }): Promise<void>;

  /**
   * Remove all values stored by this plugin.
   */
  clear(): Promise<void>;
}
