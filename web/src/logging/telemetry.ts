/**
 * Отправка клиентской телеметрии на backend:
 *  - партии логов (POST /client-logs/batch) с backoff и очередью в localStorage;
 *  - снимок сессии (POST /client/session);
 *  - активность (POST /client/activity) для online/last-activity.
 *
 * Успешные ping/health не отправляются (heartbeat-шум). Секреты уже замаскированы
 * логгером; backend дополнительно маскирует повторно.
 */
import { logger, uuid, type LogEntry } from './logger';
import { browserInfo } from './diagnostics';
import { isStandalone, isIOS, isAndroid } from '@/lib/platform';
import { APP_VERSION } from '@/version';
import { useAuthStore } from '@/store/authStore';
import {
  sendClientLogs,
  sendClientActivity,
  sendClientSession,
  type ClientLogWire,
} from '@/api/telemetry';

const SESSION_KEY = 'foodscanner.session.id';
const PENDING_KEY = 'foodscanner.tx.pending';
const MAX_PENDING = 2000;
const CHUNK = 200;
const FLUSH_INTERVAL_MS = 30_000;
const ACTIVITY_INTERVAL_MS = 30_000; // heartbeat присутствия (онлайн-окно на сервере — 5 мин)

// ── Идентификатор сессии (стабилен между перезагрузками вкладки) ──
function loadSessionId(): string {
  try {
    const existing = localStorage.getItem(SESSION_KEY);
    if (existing) return existing;
    const id = uuid();
    localStorage.setItem(SESSION_KEY, id);
    return id;
  } catch {
    return uuid();
  }
}
export const sessionId = loadSessionId();

// ── Очередь доставки ──
let pending: ClientLogWire[] = loadPending();
let persistTimer: ReturnType<typeof setTimeout> | null = null;

function loadPending(): ClientLogWire[] {
  try {
    const raw = localStorage.getItem(PENDING_KEY);
    const arr = raw ? JSON.parse(raw) : [];
    return Array.isArray(arr) ? arr.slice(-MAX_PENDING) : [];
  } catch {
    return [];
  }
}
function persistPending(): void {
  if (persistTimer) return;
  persistTimer = setTimeout(() => {
    persistTimer = null;
    try {
      localStorage.setItem(PENDING_KEY, JSON.stringify(pending.slice(-MAX_PENDING)));
    } catch {
      /* квота — не критично */
    }
  }, 1000);
}

// ── Маппинг записи лога в wire-формат + извлечение trace-полей ──
function str(v: unknown): string | undefined {
  return typeof v === 'string' ? v : undefined;
}
function num(v: unknown): number | undefined {
  return typeof v === 'number' ? v : undefined;
}

export function toWire(e: LogEntry): ClientLogWire {
  const d =
    e.details && typeof e.details === 'object' && !Array.isArray(e.details)
      ? (e.details as Record<string, unknown>)
      : undefined;
  return {
    id: e.id,
    timestamp: e.timestamp,
    level: e.level,
    category: e.category,
    message: e.message,
    event: str(d?.event),
    screen: str(d?.screen),
    correlationId: str(d?.correlationId),
    requestId: str(d?.requestId),
    apiMethod: str(d?.apiMethod),
    apiPath: str(d?.apiPath),
    httpStatus: num(d?.httpStatus),
    durationMs: num(d?.durationMs ?? d?.ms),
    barcode: str(d?.barcode),
    draftId: str(d?.draftId),
    catalogEntryId: str(d?.catalogEntryId),
    photoId: str(d?.photoId),
    metadata: e.details,
  };
}

/** Успешные ping/health — шум, не отправляем (зеркало серверной политики). */
export function isHealthNoise(w: ClientLogWire): boolean {
  const p = (w.apiPath ?? '').toLowerCase();
  const isHealth = p.endsWith('/ping') || p.endsWith('/health');
  if (!isHealth) return false;
  if (w.httpStatus != null && w.httpStatus >= 400) return false;
  if (w.httpStatus == null && (w.level === 'WARN' || w.level === 'ERROR')) return false;
  return true;
}

// ── Отправка с backoff ──
let inFlight = false;
let failCount = 0;
let failUntil = 0;
let flushTimer: ReturnType<typeof setTimeout> | null = null;

function canSend(): boolean {
  if (!useAuthStore.getState().accessToken) return false;
  if (typeof navigator !== 'undefined' && navigator.onLine === false) return false;
  return true;
}

async function flush(): Promise<void> {
  if (inFlight || Date.now() < failUntil || pending.length === 0 || !canSend()) return;
  const chunk = pending.slice(0, CHUNK);
  inFlight = true;
  try {
    await sendClientLogs({ sessionId, clientVersion: APP_VERSION, pwaVersion: APP_VERSION, logs: chunk });
    pending.splice(0, chunk.length);
    persistPending();
    failCount = 0;
    if (pending.length > 0) scheduleFlush(500);
  } catch {
    failCount += 1;
    failUntil = Date.now() + Math.min(60_000, 2_000 * 2 ** Math.min(failCount, 5));
  } finally {
    inFlight = false;
  }
}

function scheduleFlush(delay: number): void {
  if (flushTimer) return;
  flushTimer = setTimeout(() => {
    flushTimer = null;
    void flush();
  }, delay);
}

/** Принудительная отправка (например, при открытии экрана диагностики). */
export function flushTelemetry(): void {
  void flush();
}

// ── Сессия и активность ──
let sessionSent = false;

function reportSession(): void {
  const nav = typeof navigator !== 'undefined' ? navigator : undefined;
  sessionSent = true;
  void sendClientSession({
    sessionId,
    clientVersion: APP_VERSION,
    pwaVersion: APP_VERSION,
    browser: browserInfo(),
    os: isIOS() ? 'iOS' : isAndroid() ? 'Android' : 'Desktop',
    deviceType: isIOS() || isAndroid() ? 'mobile' : 'desktop',
    language: nav?.language,
    timezone: tryTimezone(),
    screenWidth: typeof screen !== 'undefined' ? screen.width : undefined,
    screenHeight: typeof screen !== 'undefined' ? screen.height : undefined,
    hardwareConcurrency: nav?.hardwareConcurrency,
    deviceMemory: (nav as unknown as { deviceMemory?: number })?.deviceMemory,
    networkStatus: nav?.onLine === false ? 'offline' : 'online',
    standalone: isStandalone(),
  }).catch(() => {
    sessionSent = false; // повторим позже
  });
}

function tryTimezone(): string | undefined {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone;
  } catch {
    return undefined;
  }
}

function activityTick(): void {
  if (!canSend()) return;
  if (!sessionSent) reportSession();
  void sendClientActivity({
    sessionId,
    screen: typeof location !== 'undefined' ? location.pathname : undefined,
    online: typeof navigator !== 'undefined' ? navigator.onLine : undefined,
    timestamp: new Date().toISOString(),
  }).catch(() => undefined);
}

// ── Запуск ──
let started = false;

export function startTelemetry(): void {
  if (started || typeof window === 'undefined') return;
  started = true;

  logger.onAppend((e) => {
    const w = toWire(e);
    if (isHealthNoise(w)) return;
    pending.push(w);
    if (pending.length > MAX_PENDING) pending.splice(0, pending.length - MAX_PENDING);
    persistPending();
    if (e.level === 'WARN' || e.level === 'ERROR') scheduleFlush(1000);
    else if (pending.length >= 50) scheduleFlush(0);
  });

  window.setInterval(() => void flush(), FLUSH_INTERVAL_MS);
  window.setInterval(activityTick, ACTIVITY_INTERVAL_MS);
  window.addEventListener('online', () => {
    activityTick();
    void flush();
  });
  // Вернулись на вкладку/приложение — сразу обновляем присутствие.
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') activityTick();
  });
  window.addEventListener('focus', activityTick);

  // Первичная сессия + первый heartbeat сразу (чтобы присутствие появилось без задержки).
  window.setTimeout(() => {
    if (canSend() && !sessionSent) reportSession();
    activityTick();
    void flush();
  }, 1500);
}
