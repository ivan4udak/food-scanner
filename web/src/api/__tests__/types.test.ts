import { describe, it, expect } from 'vitest';
import {
  HealthResponseSchema,
  ScanResponseSchema,
  AddPhotoResponseSchema,
  CatalogEntrySchema,
  REQUIRED_PHOTO_TYPES,
} from '@/api/types';

describe('API schemas (zod)', () => {
  it('парсит HealthResponse как в /health', () => {
    const r = HealthResponseSchema.safeParse({
      status: 'OK',
      backend: 'UP',
      storage: 'UP',
      timestamp: '2026-06-04T10:00:00Z',
    });
    expect(r.success).toBe(true);
  });

  it('принимает ScanResponse NEW/EXISTS и отклоняет мусор', () => {
    expect(ScanResponseSchema.safeParse({ status: 'NEW', draftId: crypto.randomUUID() }).success).toBe(true);
    expect(ScanResponseSchema.safeParse({ status: 'EXISTS', draftId: null }).success).toBe(true);
    expect(ScanResponseSchema.safeParse({ status: 'WAT', draftId: null }).success).toBe(false);
  });

  it('парсит AddPhotoResponse с missingTypes', () => {
    const r = AddPhotoResponseSchema.safeParse({
      uploadedCount: 1,
      requiredCount: 4,
      missingTypes: ['BARCODE', 'INGREDIENTS', 'NUTRITION'],
      complete: false,
    });
    expect(r.success).toBe(true);
  });

  it('парсит CatalogEntry с фото', () => {
    const r = CatalogEntrySchema.safeParse({
      id: crypto.randomUUID(),
      barcode: '4607038310042',
      contributorId: crypto.randomUUID(),
      photos: [{ id: crypto.randomUUID(), type: 'FRONT', storageKey: 'photos/abc.jpg' }],
      createdAt: '2026-06-04T10:00:00Z',
    });
    expect(r.success).toBe(true);
  });

  it('обязательные типы фото = 4 (BARCODE/FRONT/INGREDIENTS/NUTRITION)', () => {
    expect(REQUIRED_PHOTO_TYPES).toEqual(['BARCODE', 'FRONT', 'INGREDIENTS', 'NUTRITION']);
  });
});
