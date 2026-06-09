import { describe, it, expect } from 'vitest';
import { toWire, isHealthNoise } from '@/logging/telemetry';
import type { LogEntry } from '@/logging/logger';

function entry(partial: Partial<LogEntry>): LogEntry {
  return {
    id: 'id-1',
    timestamp: '2026-06-08T10:00:00.000Z',
    level: 'INFO',
    category: 'NETWORK',
    message: 'm',
    ...partial,
  } as LogEntry;
}

describe('toWire', () => {
  it('извлекает trace-поля из details', () => {
    const details = {
      correlationId: 'corr-1',
      apiMethod: 'POST',
      apiPath: '/api/v1/scan',
      httpStatus: 200,
      durationMs: 180,
      barcode: '460',
    };
    const w = toWire(entry({ category: 'SCAN', message: 'Scan result NEW', details }));
    expect(w.id).toBe('id-1');
    expect(w.correlationId).toBe('corr-1');
    expect(w.apiMethod).toBe('POST');
    expect(w.apiPath).toBe('/api/v1/scan');
    expect(w.httpStatus).toBe(200);
    expect(w.durationMs).toBe(180);
    expect(w.barcode).toBe('460');
    expect(w.metadata).toEqual(details);
  });

  it('переносит ms как durationMs', () => {
    const w = toWire(entry({ details: { ms: 42 } }));
    expect(w.durationMs).toBe(42);
  });
});

describe('isHealthNoise', () => {
  it('успешный ping/health — шум', () => {
    expect(isHealthNoise(toWire(entry({ details: { apiPath: '/api/v1/ping', httpStatus: 200 } })))).toBe(true);
    expect(isHealthNoise(toWire(entry({ details: { apiPath: '/api/v1/health', httpStatus: 200 } })))).toBe(true);
  });

  it('старт-лог ping без статуса (DEBUG) — шум', () => {
    expect(isHealthNoise(toWire(entry({ level: 'DEBUG', details: { apiPath: '/api/v1/ping' } })))).toBe(true);
  });

  it('ошибочный ping — не шум', () => {
    expect(isHealthNoise(toWire(entry({ level: 'WARN', details: { apiPath: '/api/v1/ping', httpStatus: 503 } })))).toBe(false);
    expect(isHealthNoise(toWire(entry({ level: 'ERROR', details: { apiPath: '/api/v1/ping' } })))).toBe(false);
  });

  it('обычный запрос — не шум', () => {
    expect(isHealthNoise(toWire(entry({ details: { apiPath: '/api/v1/scan', httpStatus: 200 } })))).toBe(false);
  });
});
