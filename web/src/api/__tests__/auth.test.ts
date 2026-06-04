import { describe, it, expect, afterEach } from 'vitest';
import type { AxiosAdapter } from 'axios';
import { api } from '@/api/client';
import { login, register } from '@/api/auth';

const realAdapter = api.defaults.adapter;

/** Подменяет адаптер фиксированным ответом (status/data). */
function mockResponse(status: number, data: unknown) {
  const adapter: AxiosAdapter = async (config) =>
    Promise.resolve({ data, status, statusText: '', headers: {}, config });
  api.defaults.adapter = adapter;
}

afterEach(() => {
  api.defaults.adapter = realAdapter;
});

describe('login — маппинг статусов', () => {
  it('200 OK → kind:ok с сессией', async () => {
    mockResponse(200, {
      status: 'OK',
      contributorId: crypto.randomUUID(),
      username: 'alice',
      accessToken: 'a',
      refreshToken: 'r',
    });
    const out = await login('alice', 'secret1');
    expect(out.kind).toBe('ok');
    if (out.kind === 'ok') expect(out.session.username).toBe('alice');
  });

  it('200 RECOVERY → kind:recovery', async () => {
    mockResponse(200, { status: 'RECOVERY', username: 'alice' });
    const out = await login('alice', 'x');
    expect(out.kind).toBe('recovery');
  });

  it('404 → kind:notFound', async () => {
    mockResponse(404, { status: 'NOT_FOUND' });
    expect((await login('ghost', 'x')).kind).toBe('notFound');
  });

  it('401 → kind:invalid с сообщением', async () => {
    mockResponse(401, { status: 'INVALID', message: 'Неверный логин или пароль' });
    const out = await login('alice', 'bad');
    expect(out.kind).toBe('invalid');
    if (out.kind === 'invalid') expect(out.message).toMatch(/Неверный/);
  });

  it('423 → kind:locked', async () => {
    mockResponse(423, { status: 'LOCKED', message: 'Аккаунт временно заблокирован' });
    expect((await login('alice', 'x')).kind).toBe('locked');
  });
});

describe('register', () => {
  it('201 → сессия', async () => {
    mockResponse(201, {
      status: 'OK',
      contributorId: crypto.randomUUID(),
      username: 'bob',
      accessToken: 'a',
      refreshToken: 'r',
    });
    const s = await register('bob', 'pass1234');
    expect(s.username).toBe('bob');
  });

  it('409 → ошибка «Логин уже занят»', async () => {
    mockResponse(409, { status: 409, message: 'conflict' });
    await expect(register('bob', 'pass1234')).rejects.toThrow(/занят/);
  });
});
