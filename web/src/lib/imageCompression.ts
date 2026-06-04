import imageCompression from 'browser-image-compression';

/** Параметры сжатия повторяют iOS-клиент (Блок 8): ≤1920 по большей стороне, JPEG ~≤0.8МБ. */
export const MAX_DIMENSION = 1920;
export const TARGET_MAX_MB = 0.8;

export function compressionOptions(maxSizeMB: number = TARGET_MAX_MB) {
  return {
    maxWidthOrHeight: MAX_DIMENSION,
    maxSizeMB,
    useWebWorker: true,
    fileType: 'image/jpeg' as const,
    initialQuality: 0.85,
  };
}

/** Сжимает изображение перед загрузкой. Принимает File/Blob, отдаёт JPEG-Blob. */
export async function compressImage(file: Blob, maxSizeMB: number = TARGET_MAX_MB): Promise<Blob> {
  const asFile =
    file instanceof File ? file : new File([file], 'photo.jpg', { type: file.type || 'image/jpeg' });
  return imageCompression(asFile, compressionOptions(maxSizeMB));
}

/**
 * Дата съёмки: EXIF DateTimeOriginal (как iOS читает captured_at ДО сжатия),
 * иначе lastModified файла, иначе null.
 */
export async function readCapturedAt(file: Blob): Promise<Date | null> {
  try {
    const buf = await file.arrayBuffer();
    const fromExif = parseExifDateTimeOriginal(buf);
    if (fromExif) return fromExif;
  } catch {
    /* нет/битый EXIF — не критично */
  }
  const lm = (file as File).lastModified;
  return typeof lm === 'number' && lm > 0 ? new Date(lm) : null;
}

/** Минимальный разбор EXIF: ищет DateTimeOriginal (0x9003), иначе DateTime (0x0132). */
function parseExifDateTimeOriginal(buffer: ArrayBuffer): Date | null {
  const view = new DataView(buffer);
  if (view.byteLength < 4 || view.getUint16(0) !== 0xffd8) return null; // не JPEG

  let offset = 2;
  while (offset + 4 < view.byteLength) {
    const marker = view.getUint16(offset);
    if ((marker & 0xff00) !== 0xff00) break;
    const size = view.getUint16(offset + 2);
    if (marker === 0xffe1) {
      // APP1 — EXIF
      const exifStart = offset + 4;
      if (view.getUint32(exifStart) === 0x45786966) {
        return readTiffDateTime(view, exifStart + 6);
      }
    }
    offset += 2 + size;
  }
  return null;
}

function readTiffDateTime(view: DataView, tiff: number): Date | null {
  const little = view.getUint16(tiff) === 0x4949; // 'II'
  const u16 = (o: number) => view.getUint16(o, little);
  const u32 = (o: number) => view.getUint32(o, little);

  const ifd0 = tiff + u32(tiff + 4);
  const findInIfd = (ifd: number, tag: number): number | null => {
    const count = u16(ifd);
    for (let i = 0; i < count; i++) {
      const entry = ifd + 2 + i * 12;
      if (u16(entry) === tag) return entry;
    }
    return null;
  };
  const readAscii = (entry: number): string => {
    const len = u32(entry + 4);
    const valOffset = len > 4 ? tiff + u32(entry + 8) : entry + 8;
    let s = '';
    for (let i = 0; i < len - 1; i++) s += String.fromCharCode(view.getUint8(valOffset + i));
    return s;
  };

  // EXIF sub-IFD (tag 0x8769) → DateTimeOriginal (0x9003)
  const exifPtr = findInIfd(ifd0, 0x8769);
  if (exifPtr) {
    const exifIfd = tiff + u32(exifPtr + 8);
    const dto = findInIfd(exifIfd, 0x9003);
    if (dto) return parseExifDate(readAscii(dto));
  }
  // fallback: DateTime в IFD0
  const dt = findInIfd(ifd0, 0x0132);
  if (dt) return parseExifDate(readAscii(dt));
  return null;
}

/** "YYYY:MM:DD HH:MM:SS" → Date. */
function parseExifDate(s: string): Date | null {
  const m = s.match(/^(\d{4}):(\d{2}):(\d{2}) (\d{2}):(\d{2}):(\d{2})/);
  if (!m) return null;
  const [, y, mo, d, h, mi, se] = m;
  const date = new Date(Number(y), Number(mo) - 1, Number(d), Number(h), Number(mi), Number(se));
  return Number.isNaN(date.getTime()) ? null : date;
}
