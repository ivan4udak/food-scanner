import { api } from '@/api/client';

/** Запрос, который не логируется (иначе телеметрия логировала бы саму себя). */
const SILENT = { _silent: true } as unknown as Record<string, never>;

export interface ClientLogWire {
  id: string;
  timestamp: string;
  level: string;
  category: string;
  event?: string;
  message: string;
  screen?: string;
  metadata?: unknown;
  durationMs?: number;
  stackTrace?: string;
  correlationId?: string;
  requestId?: string;
  barcode?: string;
  draftId?: string;
  catalogEntryId?: string;
  photoId?: string;
  apiMethod?: string;
  apiPath?: string;
  httpStatus?: number;
}

export interface ClientLogBatch {
  sessionId: string;
  clientVersion?: string;
  pwaVersion?: string;
  logs: ClientLogWire[];
}

export interface ClientSessionPayload {
  sessionId: string;
  clientVersion?: string;
  pwaVersion?: string;
  browser?: string;
  os?: string;
  deviceType?: string;
  language?: string;
  timezone?: string;
  screenWidth?: number;
  screenHeight?: number;
  hardwareConcurrency?: number;
  deviceMemory?: number;
  networkStatus?: string;
  standalone?: boolean;
}

export interface ClientActivityPayload {
  sessionId: string;
  screen?: string;
  online?: boolean;
  timestamp?: string;
}

/** POST /client-logs/batch → число принятых. Бросает при ошибке (для backoff в шиппере). */
export async function sendClientLogs(batch: ClientLogBatch): Promise<number> {
  const res = await api.post('/client-logs/batch', batch, SILENT);
  return (res.data?.accepted as number) ?? 0;
}

export async function sendClientSession(payload: ClientSessionPayload): Promise<void> {
  await api.post('/client/session', payload, SILENT);
}

export async function sendClientActivity(payload: ClientActivityPayload): Promise<void> {
  await api.post('/client/activity', payload, SILENT);
}
