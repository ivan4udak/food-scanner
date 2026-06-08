import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { logger } from '@/logging/logger';
import { buildDiagnosticsText, buildFullLog, browserInfo } from '@/logging/diagnostics';

beforeEach(() => {
  vi.spyOn(console, 'info').mockImplementation(() => {});
  logger.clear();
  logger.setLevel('TRACE');
});
afterEach(() => vi.restoreAllMocks());

describe('экспорт диагностики', () => {
  it('содержит шапку (Version/Browser/Network) и строки логов', () => {
    logger.info('AUTH', 'Login success', 'user=ivan');
    const txt = buildDiagnosticsText('В сети', 500);
    expect(txt).toContain('Food Scanner Diagnostics');
    expect(txt).toContain('Version:');
    expect(txt).toContain('Browser:');
    expect(txt).toContain('Network: В сети');
    expect(txt).toContain('AUTH Login success');
  });

  it('экспорт полного лога не содержит секретов', () => {
    logger.info('API', 'login', { password: 'supersecret' });
    expect(buildFullLog('offline')).not.toContain('supersecret');
  });

  it('browserInfo возвращает непустую строку', () => {
    expect(typeof browserInfo()).toBe('string');
    expect(browserInfo().length).toBeGreaterThan(0);
  });
});
