import { WebPlugin } from '@capacitor/core';

import type { SecureStoragePlugin } from './definitions';

/**
 * The web has no secure storage facility. Rather than silently falling back to
 * localStorage (which would be a security vulnerability), every method rejects
 * with an `UNAVAILABLE` error.
 */
export class SecureStorageWeb extends WebPlugin implements SecureStoragePlugin {
  async set(_options: { key: string; value: string }): Promise<void> {
    throw this.unavailable('SecureStorage is not supported on the web');
  }

  async get(_options: { key: string }): Promise<{ value: string | null }> {
    throw this.unavailable('SecureStorage is not supported on the web');
  }

  async has(_options: { key: string }): Promise<{ value: boolean }> {
    throw this.unavailable('SecureStorage is not supported on the web');
  }

  async remove(_options: { key: string }): Promise<void> {
    throw this.unavailable('SecureStorage is not supported on the web');
  }

  async clear(): Promise<void> {
    throw this.unavailable('SecureStorage is not supported on the web');
  }
}
