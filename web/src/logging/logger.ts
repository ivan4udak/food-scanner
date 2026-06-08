/**
 * Клиентское логирование: кольцевой буфер в памяти (5000) + хвост в localStorage (1000).
 * Токены/пароли НЕ логируются (маскируются). Работает в dev и prod.
 */

export type LogLevel = 'TRACE' | 'DEBUG' | 'INFO' | 'WARN' | 'ERROR';

export const LOG_CATEGORIES = [
  'AUTH', 'API', 'NETWORK', 'SCAN', 'PHOTO', 'CATALOG', 'HEALTH', 'PWA', 'UI', 'SYSTEM',
] as const;
export type LogCategory = (typeof LOG_CATEGORIES)[number];

export interface LogEntry {
  timestamp: string; // ISO-8601
  level: LogLevel;
  category: LogCategory;
  message: string;
  details?: unknown;
}

const LEVEL_ORDER: Record<LogLevel, number> = { TRACE: 10, DEBUG: 20, INFO: 30, WARN: 40, ERROR: 50 };

const MEM_CAP = 5000;
const LS_CAP = 1000;
const LS_KEY = 'foodscanner.logs';
const LS_LEVEL_KEY = 'foodscanner.log.level';

// ── Маскировка секретов ───────────────────────────────────────────────────
const SECRET_KEY = /^(password|new_?password|access[_-]?token|refresh[_-]?token|token|authorization)$/i;

function maskBearer(s: string): string {
  return s
    .replace(/Bearer\s+[A-Za-z0-9._~+/=-]+/gi, 'Bearer ********')
    .replace(/eyJ[A-Za-z0-9._-]{10,}/g, '********'); // «голый» JWT в строке
}

/** Рекурсивно маскирует пароли/токены/Authorization в произвольных details. */
export function maskSecrets(value: unknown, depth = 0): unknown {
  if (depth > 6 || value == null) return value;
  if (typeof value === 'string') return maskBearer(value);
  if (Array.isArray(value)) return value.map((v) => maskSecrets(v, depth + 1));
  if (typeof value === 'object') {
    const out: Record<string, unknown> = {};
    for (const [k, v] of Object.entries(value as Record<string, unknown>)) {
      out[k] = SECRET_KEY.test(k) ? '********' : maskSecrets(v, depth + 1);
    }
    return out;
  }
  return value;
}

// ── Буфер + запись ─────────────────────────────────────────────────────────
function isLevel(v: string | null): v is LogLevel {
  return v === 'TRACE' || v === 'DEBUG' || v === 'INFO' || v === 'WARN' || v === 'ERROR';
}

function defaultLevel(): LogLevel {
  try {
    const saved = localStorage.getItem(LS_LEVEL_KEY);
    if (isLevel(saved)) return saved;
  } catch {
    /* ignore */
  }
  // dev: DEBUG, prod: INFO
  return import.meta.env.DEV ? 'DEBUG' : 'INFO';
}

class Logger {
  private buf: LogEntry[] = [];
  private minLevel: LogLevel = defaultLevel();
  private persistTimer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    this.restore();
  }

  setLevel(level: LogLevel): void {
    this.minLevel = level;
    try {
      localStorage.setItem(LS_LEVEL_KEY, level);
    } catch {
      /* ignore */
    }
  }
  getLevel(): LogLevel {
    return this.minLevel;
  }

  log(level: LogLevel, category: LogCategory, message: string, details?: unknown): void {
    if (LEVEL_ORDER[level] < LEVEL_ORDER[this.minLevel]) return;
    const entry: LogEntry = {
      timestamp: new Date().toISOString(),
      level,
      category,
      message,
      ...(details === undefined ? {} : { details: maskSecrets(details) }),
    };
    this.buf.push(entry);
    if (this.buf.length > MEM_CAP) this.buf.splice(0, this.buf.length - MEM_CAP);
    this.toConsole(entry);
    this.schedulePersist();
  }

  trace(c: LogCategory, m: string, d?: unknown) { this.log('TRACE', c, m, d); }
  debug(c: LogCategory, m: string, d?: unknown) { this.log('DEBUG', c, m, d); }
  info(c: LogCategory, m: string, d?: unknown)  { this.log('INFO', c, m, d); }
  warn(c: LogCategory, m: string, d?: unknown)  { this.log('WARN', c, m, d); }
  error(c: LogCategory, m: string, d?: unknown) { this.log('ERROR', c, m, d); }

  getLogs(): LogEntry[] {
    return this.buf.slice();
  }
  recent(n: number): LogEntry[] {
    return this.buf.slice(-n);
  }
  count(): number {
    return this.buf.length;
  }
  clear(): void {
    this.buf = [];
    if (this.persistTimer) {
      clearTimeout(this.persistTimer);
      this.persistTimer = null;
    }
    try {
      localStorage.removeItem(LS_KEY);
    } catch {
      /* ignore */
    }
  }

  private toConsole(e: LogEntry): void {
    const line = formatLogLine(e, false);
    const fn =
      e.level === 'ERROR' ? console.error
      : e.level === 'WARN' ? console.warn
      : e.level === 'INFO' ? console.info
      : console.debug;
    if (e.details !== undefined) fn(line, e.details);
    else fn(line);
  }

  private schedulePersist(): void {
    if (this.persistTimer) return;
    this.persistTimer = setTimeout(() => {
      this.persistTimer = null;
      try {
        localStorage.setItem(LS_KEY, JSON.stringify(this.buf.slice(-LS_CAP)));
      } catch {
        /* квота/недоступно — не критично */
      }
    }, 1000);
  }

  private restore(): void {
    try {
      const raw = localStorage.getItem(LS_KEY);
      if (!raw) return;
      const arr = JSON.parse(raw);
      if (Array.isArray(arr)) this.buf = arr.slice(-MEM_CAP);
    } catch {
      /* ignore */
    }
  }
}

// ── Форматирование ─────────────────────────────────────────────────────────
function pad(n: number, w = 2): string {
  return String(n).padStart(w, '0');
}

/** `[12:14:55.123] INFO AUTH message` (+ details JSON, если withDetails). */
export function formatLogLine(e: LogEntry, withDetails = true): string {
  const d = new Date(e.timestamp);
  const t = `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}.${pad(d.getMilliseconds(), 3)}`;
  let line = `[${t}] ${e.level} ${e.category} ${e.message}`;
  if (withDetails && e.details !== undefined) {
    try {
      line += ` ${JSON.stringify(e.details)}`;
    } catch {
      /* ignore */
    }
  }
  return line;
}

export const logger = new Logger();
