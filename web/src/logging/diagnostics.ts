/**
 * Сбор диагностики: сведения об устройстве/браузере/сети + хвост логов.
 * Для копирования (последние 500) и экспорта в файл (полный лог).
 */
import { logger, formatLogLine, type LogEntry } from './logger';
import { isStandalone, isIOS, isAndroid } from '@/lib/platform';
import { APP_VERSION, PLATFORM } from '@/version';
import { API_BASE } from '@/api/client';

/** Браузер и версия из User-Agent (Chrome/Edge/Opera/Firefox/Safari). */
export function browserInfo(): string {
  const ua = (typeof navigator !== 'undefined' && navigator.userAgent) || '';
  let m: RegExpMatchArray | null;
  if ((m = ua.match(/Edg\/(\d+)/))) return `Edge ${m[1]}`;
  if ((m = ua.match(/OPR\/(\d+)/))) return `Opera ${m[1]}`;
  if ((m = ua.match(/Firefox\/(\d+)/))) return `Firefox ${m[1]}`;
  if ((m = ua.match(/Chrome\/(\d+)/))) return `Chrome ${m[1]}`;
  if ((m = ua.match(/Version\/(\d+)[\d.]*\s+(?:Mobile\/\w+\s+)?Safari/))) return `Safari ${m[1]}`;
  if (/Safari/.test(ua)) return 'Safari';
  return 'Unknown';
}

function platformLabel(): string {
  if (isIOS()) return 'iOS';
  if (isAndroid()) return 'Android';
  return 'Desktop';
}

/** Полный backend-URL базового API. */
export function backendUrl(): string {
  const origin = typeof location !== 'undefined' ? location.origin : '';
  return `${origin}${API_BASE}`;
}

/** Шапка диагностики (сведения об устройстве/браузере/сети). */
export function diagnosticsHeader(connectionLabel: string): string {
  return [
    'Food Scanner Diagnostics',
    '',
    `Version: ${APP_VERSION} (${PLATFORM})`,
    `Browser: ${browserInfo()}`,
    `Platform: ${platformLabel()}`,
    `Standalone: ${isStandalone()}`,
    `Backend: ${backendUrl()}`,
    `Network: ${connectionLabel}`,
    `Online: ${typeof navigator !== 'undefined' ? navigator.onLine : 'n/a'}`,
    `Time: ${new Date().toISOString()}`,
    `Logs total: ${logger.count()}`,
    `User-Agent: ${(typeof navigator !== 'undefined' && navigator.userAgent) || ''}`,
  ].join('\n');
}

function logsBlock(entries: LogEntry[]): string {
  return entries.map((e) => formatLogLine(e)).join('\n');
}

/** Текст для кнопки «Скопировать диагностику»: шапка + последние `n` логов (по умолчанию 500). */
export function buildDiagnosticsText(connectionLabel: string, n = 500): string {
  const entries = logger.recent(n);
  return `${diagnosticsHeader(connectionLabel)}\n\nLogs (${entries.length} of ${logger.count()}):\n${logsBlock(entries)}\n`;
}

/** Полный лог для экспорта в файл. */
export function buildFullLog(connectionLabel: string): string {
  const entries = logger.getLogs();
  return `${diagnosticsHeader(connectionLabel)}\n\nLogs (full, ${entries.length}):\n${logsBlock(entries)}\n`;
}

/** Скачивает полную диагностику как food-scanner-log.txt. */
export function downloadLog(connectionLabel: string): void {
  const blob = new Blob([buildFullLog(connectionLabel)], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'food-scanner-log.txt';
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
