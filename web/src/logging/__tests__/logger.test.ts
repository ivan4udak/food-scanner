import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { logger, maskSecrets } from '@/logging/logger';

beforeEach(() => {
  vi.spyOn(console, 'info').mockImplementation(() => {});
  vi.spyOn(console, 'warn').mockImplementation(() => {});
  vi.spyOn(console, 'error').mockImplementation(() => {});
  vi.spyOn(console, 'debug').mockImplementation(() => {});
  logger.clear();
  logger.setLevel('TRACE');
});
afterEach(() => {
  vi.restoreAllMocks();
  localStorage.clear();
});

describe('кольцевой буфер', () => {
  it('хранит не более 5000 записей, вытесняя старые', () => {
    for (let i = 0; i < 5005; i++) logger.info('SYSTEM', `m${i}`);
    expect(logger.count()).toBe(5000);
    const all = logger.getLogs();
    expect(all[0].message).toBe('m5'); // первые 5 вытеснены
    expect(all[all.length - 1].message).toBe('m5004');
  });
});

describe('уровни логирования', () => {
  it('фильтрует записи ниже minLevel', () => {
    logger.setLevel('WARN');
    logger.trace('SYSTEM', 't');
    logger.debug('SYSTEM', 'd');
    logger.info('SYSTEM', 'i');
    logger.warn('SYSTEM', 'w');
    logger.error('SYSTEM', 'e');
    expect(logger.getLogs().map((e) => e.level)).toEqual(['WARN', 'ERROR']);
  });
});

describe('localStorage', () => {
  it('сохраняет хвост логов в localStorage (debounced)', () => {
    vi.useFakeTimers();
    logger.info('AUTH', 'persisted-line');
    vi.advanceTimersByTime(1100);
    const raw = localStorage.getItem('foodscanner.logs');
    expect(raw).toBeTruthy();
    const arr = JSON.parse(raw as string);
    expect(arr.some((e: { message: string }) => e.message === 'persisted-line')).toBe(true);
    vi.useRealTimers();
  });
});

describe('маскировка секретов', () => {
  it('maskSecrets маскирует пароль/токены/Bearer и не трогает остальное', () => {
    const m = maskSecrets({
      password: 'p',
      accessToken: 'a',
      authorization: 'Bearer abc.def',
      nested: { refreshToken: 'r' },
      ok: 'fine',
    }) as Record<string, unknown> & { nested: Record<string, unknown> };
    expect(m.password).toBe('********');
    expect(m.accessToken).toBe('********');
    expect(m.authorization).toBe('********');
    expect(m.nested.refreshToken).toBe('********');
    expect(m.ok).toBe('fine');
  });

  it('маскирует Bearer внутри обычной строки', () => {
    expect(maskSecrets('hdr Bearer eyJabc.def.ghi end')).toContain('Bearer ********');
    expect(maskSecrets('hdr Bearer eyJabc.def.ghi end')).not.toContain('eyJabc.def.ghi');
  });

  it('логируемые details не содержат токен', () => {
    logger.info('API', 'req', { headers: { Authorization: 'Bearer secrettoken123' }, password: 'p' });
    const e = logger.getLogs().at(-1);
    const s = JSON.stringify(e?.details);
    expect(s).not.toContain('secrettoken123');
  });
});
