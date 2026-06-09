import { describe, it, expect, vi, afterEach } from 'vitest';
import { clearAppCaches } from '@/lib/cache';

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('clearAppCaches', () => {
  it('удаляет все кэши и возвращает их число', async () => {
    const del = vi.fn().mockResolvedValue(true);
    vi.stubGlobal('caches', { keys: vi.fn().mockResolvedValue(['a', 'b', 'c']), delete: del });

    const count = await clearAppCaches();

    expect(count).toBe(3);
    expect(del).toHaveBeenCalledTimes(3);
    expect(del).toHaveBeenCalledWith('a');
  });

  it('0, если Cache Storage недоступен', async () => {
    vi.stubGlobal('caches', undefined);
    expect(await clearAppCaches()).toBe(0);
  });

  it('считает только реально удалённые', async () => {
    vi.stubGlobal('caches', {
      keys: vi.fn().mockResolvedValue(['x', 'y']),
      delete: vi.fn().mockResolvedValueOnce(true).mockResolvedValueOnce(false),
    });
    expect(await clearAppCaches()).toBe(1);
  });
});
