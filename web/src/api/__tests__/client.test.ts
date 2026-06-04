import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import axios, { AxiosError, type AxiosAdapter } from 'axios';
import { z } from 'zod';
import { api, ApiError, normalizeError, parseOrThrow } from '@/api/client';
import { useAuthStore } from '@/store/authStore';

describe('normalizeError', () => {
  it('маппит ServerError → ApiError со статусом и details', () => {
    const ax = new AxiosError('Bad', 'ERR', undefined, null, {
      status: 422,
      statusText: '',
      headers: {},
      config: {} as never,
      data: { status: 422, error: 'Unprocessable', message: 'Не собраны фото', details: ['BARCODE'] },
    });
    const e = normalizeError(ax);
    expect(e).toBeInstanceOf(ApiError);
    expect(e.status).toBe(422);
    expect(e.message).toContain('Не собраны фото');
    expect(e.message).toContain('BARCODE');
  });

  it('сетевая ошибка (нет ответа) → status 0', () => {
    const ax = new AxiosError('Network Error', 'ERR_NETWORK');
    expect(normalizeError(ax).status).toBe(0);
  });
});

describe('parseOrThrow', () => {
  const Schema = z.object({ ok: z.boolean() });
  it('возвращает данные при совпадении', () => {
    expect(parseOrThrow(Schema, { ok: true })).toEqual({ ok: true });
  });
  it('бросает ApiError 502 при несовпадении', () => {
    expect(() => parseOrThrow(Schema, { ok: 'no' })).toThrowError(ApiError);
  });
});

describe('авто-refresh на 401 + повтор запроса', () => {
  const realAdapter = api.defaults.adapter;

  beforeEach(() => {
    useAuthStore.setState({
      contributorId: crypto.randomUUID(),
      username: 'u',
      accessToken: 'old-access',
      refreshToken: 'refresh-1',
    });
  });

  afterEach(() => {
    api.defaults.adapter = realAdapter;
    vi.restoreAllMocks();
    useAuthStore.getState().signOut();
  });

  it('401 → refresh → повтор с новым токеном → 200', async () => {
    // refresh идёт «голым» axios.post — мокаем его.
    const postSpy = vi.spyOn(axios, 'post').mockResolvedValue({
      data: { status: 'OK', accessToken: 'new-access', refreshToken: 'refresh-2' },
      status: 200,
      statusText: 'OK',
      headers: {},
      config: {} as never,
    });

    let call = 0;
    const adapter: AxiosAdapter = async (config) => {
      call += 1;
      if (call === 1) {
        return Promise.reject(
          new AxiosError('Unauthorized', 'ERR', config, null, {
            status: 401,
            statusText: '',
            headers: {},
            config,
            data: { status: 'INVALID' },
          }),
        );
      }
      return Promise.resolve({ data: { ok: true }, status: 200, statusText: 'OK', headers: {}, config });
    };
    api.defaults.adapter = adapter;

    const res = await api.get('/protected');

    expect(postSpy).toHaveBeenCalledTimes(1);
    expect(call).toBe(2); // оригинал + повтор
    expect(res.status).toBe(200);
    expect(useAuthStore.getState().accessToken).toBe('new-access');
  });

  it('refresh не сработал → signOut и проброс ошибки', async () => {
    vi.spyOn(axios, 'post').mockRejectedValue(new AxiosError('no', 'ERR'));

    const adapter: AxiosAdapter = async (config) =>
      Promise.reject(
        new AxiosError('Unauthorized', 'ERR', config, null, {
          status: 401,
          statusText: '',
          headers: {},
          config,
          data: { status: 'INVALID' },
        }),
      );
    api.defaults.adapter = adapter;

    await expect(api.get('/protected')).rejects.toBeInstanceOf(ApiError);
    expect(useAuthStore.getState().accessToken).toBeNull();
  });
});
