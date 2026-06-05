import { api, ApiError, parseOrThrow } from '@/api/client';
import { useAuthStore } from '@/store/authStore';
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
  const res = await api.post('/scan', { barcodeValue, contributorId });
  return parseOrThrow(ScanResponseSchema, res.data);
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

  const res = await api.post(`/drafts/${input.draftId}/photos`, form, {
    // Пусть браузер сам выставит multipart boundary.
    headers: { 'Content-Type': undefined as unknown as string },
    onUploadProgress: (e) => {
      if (input.onProgress && e.total) input.onProgress(Math.min(1, e.loaded / e.total));
    },
  });
  return parseOrThrow(AddPhotoResponseSchema, res.data);
}

/** GET /drafts/{id} — состояние черновика (для восстановления фото). null при 404. */
export async function getDraft(draftId: string): Promise<DraftDetails | null> {
  const res = await api.get(`/drafts/${draftId}`, { validateStatus: () => true });
  if (res.status === 404) return null;
  if (res.status === 200) return parseOrThrow(DraftDetailsSchema, res.data);
  throw new ApiError(`Не удалось загрузить черновик (HTTP ${res.status})`, res.status);
}

/** POST /drafts/{id}/complete — 201. */
export async function complete(draftId: string): Promise<CompleteResponse> {
  const res = await api.post(`/drafts/${draftId}/complete`);
  return parseOrThrow(CompleteResponseSchema, res.data);
}

/** GET /entries/{barcode} — null при 404. */
export async function getEntry(barcode: string): Promise<CatalogEntry | null> {
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
