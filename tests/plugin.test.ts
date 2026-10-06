import { describe, expect, it, vi } from 'vitest';

import { SecureStorageWeb } from '../src/web';

/**
 * Minimal in-memory stand-in for the native bridge that follows the documented
 * contract. It lets us verify the public API shape and edge cases
 * (Unicode, empty values, long values, overwrite, idempotent remove).
 */
function createFakeNative() {
  const store = new Map<string, string>();
  const checkKey = (key: string) => {
    if (typeof key !== 'string' || key.length === 0) {
      throw Object.assign(new Error('Key must not be empty'), { code: 'INVALID_KEY' });
    }
  };
  return {
    async set({ key, value }: { key: string; value: string }) {
      checkKey(key);
      if (typeof value !== 'string') {
        throw Object.assign(new Error('Value must be a string'), { code: 'INVALID_VALUE' });
      }
      store.set(key, value);
    },
    async get({ key }: { key: string }) {
      checkKey(key);
      return { value: store.has(key) ? (store.get(key) as string) : null };
    },
    async has({ key }: { key: string }) {
      checkKey(key);
      return { value: store.has(key) };
    },
    async remove({ key }: { key: string }) {
      checkKey(key);
      store.delete(key);
    },
    async clear() {
      store.clear();
    },
  };
}

describe('SecureStoragePlugin contract', () => {
  it('sets, gets, checks and removes', async () => {
    const s = createFakeNative();
    await s.set({ key: 'access_token', value: 'secret-value' });
    expect(await s.get({ key: 'access_token' })).toEqual({ value: 'secret-value' });
    expect(await s.has({ key: 'access_token' })).toEqual({ value: true });
    await s.remove({ key: 'access_token' });
    expect(await s.get({ key: 'access_token' })).toEqual({ value: null });
    expect(await s.has({ key: 'access_token' })).toEqual({ value: false });
  });

  it('returns null for a missing key and remove is idempotent', async () => {
    const s = createFakeNative();
    expect(await s.get({ key: 'missing' })).toEqual({ value: null });
    await expect(s.remove({ key: 'missing' })).resolves.toBeUndefined();
  });

  it('rejects empty keys', async () => {
    const s = createFakeNative();
    await expect(s.set({ key: '', value: 'x' })).rejects.toMatchObject({ code: 'INVALID_KEY' });
    await expect(s.get({ key: '' })).rejects.toMatchObject({ code: 'INVALID_KEY' });
  });

  it('supports Unicode keys/values, empty and very long values', async () => {
    const s = createFakeNative();
    await s.set({ key: 'ключ-🔑', value: 'значение 🙂 日本語' });
    expect(await s.get({ key: 'ключ-🔑' })).toEqual({ value: 'значение 🙂 日本語' });
    await s.set({ key: 'empty', value: '' });
    expect(await s.get({ key: 'empty' })).toEqual({ value: '' });
    expect(await s.has({ key: 'empty' })).toEqual({ value: true });
    const long = 'a'.repeat(1_000_000);
    await s.set({ key: 'long', value: long });
    expect((await s.get({ key: 'long' })).value).toBe(long);
  });

  it('overwrites existing values and clears everything', async () => {
    const s = createFakeNative();
    await s.set({ key: 'k', value: 'one' });
    await s.set({ key: 'k', value: 'two' });
    expect(await s.get({ key: 'k' })).toEqual({ value: 'two' });
    await s.clear();
    expect(await s.has({ key: 'k' })).toEqual({ value: false });
  });
});

describe('SecureStorageWeb', () => {
  it('rejects every method as unsupported', async () => {
    const web = new SecureStorageWeb();
    const calls = [
      () => web.set({ key: 'k', value: 'v' }),
      () => web.get({ key: 'k' }),
      () => web.has({ key: 'k' }),
      () => web.remove({ key: 'k' }),
      () => web.clear(),
    ];
    for (const call of calls) {
      await expect(call()).rejects.toMatchObject({
        message: 'SecureStorage is not supported on the web',
        code: 'UNAVAILABLE',
      });
    }
  });
});

describe('registration', () => {
  it('registers the plugin as "SecureStorage"', async () => {
    vi.resetModules();
    const registerPlugin = vi.fn(() => ({}));
    vi.doMock('@capacitor/core', async (orig) => ({
      ...(await orig<typeof import('@capacitor/core')>()),
      registerPlugin,
    }));
    await import('../src/index');
    expect(registerPlugin).toHaveBeenCalledWith('SecureStorage', expect.anything());
  });
});
