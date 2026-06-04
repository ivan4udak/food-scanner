import { api } from '@/api/client';
import { HealthResponseSchema, type HealthResponse } from '@/api/types';

/** GET /ping — true, если сервер ответил 2xx. Публичный. */
export async function ping(timeoutMs = 4000): Promise<boolean> {
  try {
    const res = await api.get('/ping', { timeout: timeoutMs, validateStatus: () => true });
    return res.status >= 200 && res.status < 300;
  } catch {
    return false;
  }
}

/** GET /health — состояние backend + MinIO (Блок 20). null при сетевой ошибке. */
export async function health(timeoutMs = 4000): Promise<HealthResponse | null> {
  try {
    const res = await api.get('/health', { timeout: timeoutMs, validateStatus: () => true });
    if (res.status < 200 || res.status >= 300) return null;
    const parsed = HealthResponseSchema.safeParse(res.data);
    return parsed.success ? parsed.data : null;
  } catch {
    return null;
  }
}
