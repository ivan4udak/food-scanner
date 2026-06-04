import { describe, it, expect } from 'vitest';
import { compressionOptions, readCapturedAt, MAX_DIMENSION } from '@/lib/imageCompression';

describe('compressionOptions', () => {
  it('ограничивает размер 1920 и JPEG', () => {
    const o = compressionOptions();
    expect(o.maxWidthOrHeight).toBe(MAX_DIMENSION);
    expect(o.fileType).toBe('image/jpeg');
    expect(o.maxSizeMB).toBeGreaterThan(0);
  });

  it('принимает кастомный лимит МБ', () => {
    expect(compressionOptions(1.5).maxSizeMB).toBe(1.5);
  });
});

describe('readCapturedAt', () => {
  it('без EXIF и без lastModified → null', async () => {
    const blob = new Blob([new Uint8Array([1, 2, 3, 4])], { type: 'application/octet-stream' });
    expect(await readCapturedAt(blob)).toBeNull();
  });

  it('без EXIF, но с lastModified → дата файла', async () => {
    const ts = Date.UTC(2026, 4, 1, 12, 0, 0);
    const file = new File([new Uint8Array([1, 2, 3, 4])], 'photo.jpg', {
      type: 'image/jpeg',
      lastModified: ts,
    });
    const d = await readCapturedAt(file);
    expect(d?.getTime()).toBe(ts);
  });
});
