import { z } from 'zod';

/**
 * DTO и zod-схемы 1:1 с docs/API.md (Spring Boot /api/v1).
 * Схемы используются для безопасного разбора ответов: если сервер изменится,
 * `safeParse` даст явную ошибку вместо «тихого» падения.
 */

// ── Перечисления домена ───────────────────────────────────────────────
export const PHOTO_TYPES = ['BARCODE', 'FRONT', 'BACK', 'INGREDIENTS', 'NUTRITION', 'EXTRA'] as const;
export type PhotoType = (typeof PHOTO_TYPES)[number];

/** Обязательные типы фото для завершения каталога (см. API.md). */
export const REQUIRED_PHOTO_TYPES: PhotoType[] = ['BARCODE', 'FRONT', 'INGREDIENTS', 'NUTRITION'];

// ── Auth ──────────────────────────────────────────────────────────────
export const AuthResponseSchema = z.object({
  status: z.string(),
  contributorId: z.string().uuid().optional().nullable(),
  username: z.string().optional().nullable(),
  accessToken: z.string().optional().nullable(),
  refreshToken: z.string().optional().nullable(),
  message: z.string().optional().nullable(),
});
export type AuthResponse = z.infer<typeof AuthResponseSchema>;

/** Успешная сессия: профиль + пара токенов. */
export interface Session {
  contributorId: string;
  username: string;
  accessToken: string;
  refreshToken: string;
}

// ── Heartbeat / диагностика ───────────────────────────────────────────
export const PingResponseSchema = z.object({
  status: z.string(),
  timestamp: z.string(),
});
export type PingResponse = z.infer<typeof PingResponseSchema>;

export const HealthResponseSchema = z.object({
  status: z.string(), // "OK" | "DEGRADED"
  backend: z.string(), // "UP"
  storage: z.string(), // "UP" | "DOWN"
  timestamp: z.string().optional(),
});
export type HealthResponse = z.infer<typeof HealthResponseSchema>;

// ── Каталогизация ─────────────────────────────────────────────────────
export const ScanResponseSchema = z.object({
  status: z.enum(['NEW', 'EXISTS']),
  draftId: z.string().uuid().nullable(),
});
export type ScanResponse = z.infer<typeof ScanResponseSchema>;

export const AddPhotoResponseSchema = z.object({
  uploadedCount: z.number(),
  requiredCount: z.number(),
  missingTypes: z.array(z.string()),
  complete: z.boolean(),
});
export type AddPhotoResponse = z.infer<typeof AddPhotoResponseSchema>;

export const CompleteResponseSchema = z.object({
  catalogEntryId: z.string().uuid(),
  contributorCompletedCount: z.number(),
});
export type CompleteResponse = z.infer<typeof CompleteResponseSchema>;

// Состояние черновика (GET /drafts/{id}) — для восстановления фото на клиенте.
export const DraftPhotoSchema = z.object({
  type: z.string(),
  storageKey: z.string(),
  capturedAt: z.string().optional().nullable(),
});
export const DraftDetailsSchema = z.object({
  draftId: z.string(),
  barcode: z.string(),
  status: z.string(),
  photos: z.array(DraftPhotoSchema),
  uploadedCount: z.number(),
  requiredCount: z.number(),
  missingTypes: z.array(z.string()),
  complete: z.boolean(),
});
export type DraftDetails = z.infer<typeof DraftDetailsSchema>;

export const CatalogPhotoSchema = z.object({
  id: z.string().uuid(),
  type: z.string(),
  storageKey: z.string(),
  capturedAt: z.string().optional().nullable(),
});
export type CatalogPhoto = z.infer<typeof CatalogPhotoSchema>;

export const CatalogEntrySchema = z.object({
  id: z.string().uuid(),
  barcode: z.string(),
  contributorId: z.string().uuid(),
  photos: z.array(CatalogPhotoSchema),
  createdAt: z.string(),
});
export type CatalogEntry = z.infer<typeof CatalogEntrySchema>;

// ── Ошибка сервера ────────────────────────────────────────────────────
export const ServerErrorSchema = z.object({
  status: z.number(),
  error: z.string().optional(),
  message: z.string().optional(),
  details: z.array(z.string()).optional().nullable(),
  timestamp: z.string().optional(),
});
export type ServerError = z.infer<typeof ServerErrorSchema>;
