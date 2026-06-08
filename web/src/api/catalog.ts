import { api, ApiError, parseOrThrow } from '@/api/client';
import { useAuthStore } from '@/store/authStore';
import { logger } from '@/logging/logger';
import {
  AddPhotoResponseSchema,
  CatalogEntrySchema,
  CompleteResponseSchema,
  DraftDetailsSchema,
  ScanResponseSchema,
  type AddPhotoResponse,
  type CatalogEntry,
  type CompleteResponse,
  type DraftDetails,
  type PhotoType,
  type ScanResponse,
} from '@/api/types';

/**
 * POST /scan — реального пользователя backend берёт из токена, но DTO тела всё ещё
 * валидирует contributorId как @NotNull, поэтому отправляем его (id текущего юзера).
 */
export async function scan(barcodeValue: string): Promise<ScanResponse> {
  const contributorId = useAuthStore.getState().contributorId;
  logger.info('SCAN', 'Scan request started', { barcode: barcodeValue });
  const res = await api.post('/scan', { barcodeValue, contributorId });
  const parsed = parseOrThrow(ScanResponseSchema, res.data);
  logger.info('SCAN', `Scan result ${parsed.status}`, { barcode: barcodeValue, draftId: parsed.draftId });
  return parsed;
}

export interface AddPhotoInput {
  draftId: string;
  file: Blob;
  photoType: PhotoType;
  filename?: string;
  capturedAt?: Date | null;
  onProgress?: (fraction: number) => void;
}

/** POST /drafts/{id}/photos (multipart) с прогрессом отправки. */
export async function addPhoto(input: AddPhotoInput): Promise<AddPhotoResponse> {
  const form = new FormData();
  form.append('file', input.file, input.filename ?? 'photo.jpg');
  form.append('photoType', input.photoType);
  if (input.capturedAt) form.append('capturedAt', input.capturedAt.toISOString());

  logger.info('PHOTO', 'Photo upload started', {
    draftId: input.draftId,
    photoType: input.photoType,
    bytes: input.file.size,
  });
  let lastLogged = -1;
  try {
    const res = await api.post(`/drafts/${input.draftId}/photos`, form, {
      // Пусть браузер сам выставит multipart boundary.
      headers: { 'Content-Type': undefined as unknown as string },
      onUploadProgress: (e) => {
        if (e.total) {
          const frac = Math.min(1, e.loaded / e.total);
          input.onProgress?.(frac);
          const pct = Math.floor(frac * 100);
          if (pct >= lastLogged + 25 || pct === 100) {
            lastLogged = pct;
            logger.debug('PHOTO', `Photo upload progress ${pct}%`, { photoType: input.photoType });
          }
        }
      },
    });
    const parsed = parseOrThrow(AddPhotoResponseSchema, res.data);
    logger.info('PHOTO', 'Photo upload success', { draftId: input.draftId, photoType: input.photoType });
    return parsed;
  } catch (e) {
    logger.error('PHOTO', 'Photo upload failed', {
      draftId: input.draftId,
      photoType: input.photoType,
      error: e instanceof Error ? e.message : String(e),
    });
    throw e;
  }
}

/** GET /drafts/{id} — состояние черновика (для восстановления фото). null при 404. */
export async function getDraft(draftId: string): Promise<DraftDetails | null> {
  logger.info('CATALOG', 'Draft opened', { draftId });
  const res = await api.get(`/drafts/${draftId}`, { validateStatus: () => true });
  if (res.status === 404) {
    logger.warn('CATALOG', 'Draft not found', { draftId });
    return null;
  }
  if (res.status === 200) {
    const parsed = parseOrThrow(DraftDetailsSchema, res.data);
    logger.debug('CATALOG', 'Draft loaded', { draftId, photos: parsed.photos?.length });
    return parsed;
  }
  throw new ApiError(`Не удалось загрузить черновик (HTTP ${res.status})`, res.status);
}

/** POST /drafts/{id}/complete — 201. */
export async function complete(draftId: string): Promise<CompleteResponse> {
  logger.info('CATALOG', 'Draft complete requested', { draftId });
  const res = await api.post(`/drafts/${draftId}/complete`);
  const parsed = parseOrThrow(CompleteResponseSchema, res.data);
  logger.info('CATALOG', 'Draft completed', { draftId });
  return parsed;
}

/** GET /entries/{barcode} — null при 404. */
export async function getEntry(barcode: string): Promise<CatalogEntry | null> {
  logger.info('CATALOG', 'Entry viewed', { barcode });
  const res = await api.get(`/entries/${encodeURIComponent(barcode)}`, { validateStatus: () => true });
  if (res.status === 404) return null;
  if (res.status === 200) return parseOrThrow(CatalogEntrySchema, res.data);
  throw new ApiError(`Не удалось загрузить запись (HTTP ${res.status})`, res.status);
}

/**
 * Загружает фото с Bearer-заголовком и отдаёт object URL (для <img>).
 * storageKey уже содержит префикс «photos/…», поэтому путь — /photos/{storageKey}.
 * Запрос проходит через SW-кэш (CacheFirst, см. vite.config).
 */
export async function fetchPhotoObjectUrl(storageKey: string, size: 'thumb' | 'full' = 'full'): Promise<string> {
  const res = await api.get(`/photos/${storageKey}`, {
    responseType: 'blob',
    params: size === 'thumb' ? { size: 'thumb' } : undefined,
  });
  return URL.createObjectURL(res.data as Blob);
}
