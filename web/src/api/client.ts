import axios, { AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios';
import { z } from 'zod';
import { useAuthStore } from '@/store/authStore';
import { AuthResponseSchema, ServerErrorSchema } from '@/api/types';
import { logger } from '@/logging/logger';

export const API_BASE = import.meta.env.VITE_API_BASE ?? '/api/v1';

/** Нормализованная ошибка API для UI. */
export class ApiError extends Error {
  status: number;
  details?: string[];
  /** Бизнес-статус из тела (например INVALID / LOCKED / NOT_FOUND). */
  serverStatus?: string;

  constructor(message: string, status: number, details?: string[], serverStatus?: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.details = details;
    this.serverStatus = serverStatus;
  }
}

/** Приводит любую ошибку axios к ApiError с человекочитаемым сообщением. */
export function normalizeError(error: unknown): ApiError {
  if (error instanceof ApiError) return error;
  if (axios.isAxiosError(error)) {
    const ax = error as AxiosError;
    if (ax.response) {
      const parsed = ServerErrorSchema.safeParse(ax.response.data);
      if (parsed.success) {
        const e = parsed.data;
        const msg =
          e.details && e.details.length ? `${e.message ?? ''} — ${e.details.join(', ')}`.trim() : e.message;
        return new ApiError(msg || `HTTP ${e.status}`, e.status, e.details ?? undefined);
      }
      return new ApiError(`HTTP ${ax.response.status}`, ax.response.status);
    }
    return new ApiError('Нет связи с сервером', 0);
  }
  return new ApiError(error instanceof Error ? error.message : 'Неизвестная ошибка', -1);
}

/** Безопасный разбор тела ответа по zod-схеме (иначе ApiError). */
export function parseOrThrow<T>(schema: z.ZodType<T>, data: unknown): T {
  const r = schema.safeParse(data);
  if (!r.success) {
    throw new ApiError('Неверный формат ответа сервера', 502, [r.error.issues[0]?.message ?? 'schema mismatch']);
  }
  return r.data;
}

export const api: AxiosInstance = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
});

/** Метаданные запроса для измерения длительности (живут в config). */
type TimedConfig = InternalAxiosRequestConfig & { _start?: number; _retry?: boolean; _retried?: boolean };

const methodPath = (cfg?: { method?: string; url?: string }) =>
  `${(cfg?.method ?? 'GET').toUpperCase()} ${cfg?.url ?? ''}`;

/** Размер тела ответа в байтах (по content-length или длине строки). */
function responseSize(res: { headers?: Record<string, unknown>; data?: unknown }): number | undefined {
  const len = res.headers?.['content-length'];
  if (typeof len === 'string' && len) return Number(len);
  try {
    if (typeof res.data === 'string') return res.data.length;
    if (res.data) return JSON.stringify(res.data).length;
  } catch {
    /* ignore */
  }
  return undefined;
}

// Запрос: добавляем Bearer (если есть) + логируем старт.
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  (config as TimedConfig)._start = Date.now();
  logger.debug('NETWORK', `→ ${methodPath(config)}`, {
    params: config.params,
    auth: token ? 'Bearer ********' : undefined,
  });
  return config;
});

// Ответ (успех): длительность + статус + размер.
api.interceptors.response.use((res) => {
  const cfg = res.config as TimedConfig;
  const ms = cfg._start ? Date.now() - cfg._start : undefined;
  const size = responseSize(res);
  logger.info('NETWORK', `← ${res.status} ${res.statusText || 'OK'} ${methodPath(cfg)}`, {
    ms,
    size,
    retried: cfg._retried || undefined,
  });
  return res;
});

const isAuthPath = (url?: string) => !!url && url.includes('/auth/');

/** Обновление токенов отдельным «голым» запросом (без рекурсии интерсепторов). */
export async function refreshTokens(): Promise<string | null> {
  const refreshToken = useAuthStore.getState().refreshToken;
  if (!refreshToken) return null;
  logger.info('AUTH', 'Token refresh started');
  try {
    const res = await axios.post(`${API_BASE}/auth/refresh`, { refreshToken });
    const parsed = AuthResponseSchema.safeParse(res.data);
    if (parsed.success && parsed.data.accessToken && parsed.data.refreshToken) {
      useAuthStore.getState().setTokens(parsed.data.accessToken, parsed.data.refreshToken);
      logger.info('AUTH', 'Token refresh success');
      return parsed.data.accessToken;
    }
    logger.warn('AUTH', 'Token refresh: bad response');
  } catch {
    logger.warn('AUTH', 'Token refresh failed');
  }
  return null;
}

// Единая «волна» refresh при параллельных 401.
let refreshPromise: Promise<string | null> | null = null;

api.interceptors.response.use(
  (r) => r,
  async (error: AxiosError) => {
    const original = error.config as TimedConfig | undefined;
    const status = error.response?.status;
    const ms = original?._start ? Date.now() - original._start : undefined;

    if (status) {
      logger.warn('NETWORK', `← ${status} ${error.response?.statusText || ''} ${methodPath(original)}`, { ms });
    } else {
      logger.error('NETWORK', `× FAILED ${methodPath(original)}`, { ms, message: error.message });
    }

    if (status === 401 && original && !original._retry && !isAuthPath(original.url)) {
      original._retry = true;
      if (!refreshPromise) {
        refreshPromise = refreshTokens().finally(() => {
          refreshPromise = null;
        });
      }
      const newToken = await refreshPromise;
      if (newToken) {
        original.headers.Authorization = `Bearer ${newToken}`;
        original._retried = true;
        logger.info('NETWORK', `↻ retry ${methodPath(original)}`);
        return api(original);
      }
      logger.warn('AUTH', 'Logout: refresh expired');
      useAuthStore.getState().signOut(); // refresh протух → выход
    }

    return Promise.reject(normalizeError(error));
  },
);
