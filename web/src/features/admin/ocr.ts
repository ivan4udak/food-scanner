/** OCR-статусы (коды 0–5, см. docs/OCR.md) — подписи и цветовые «виды» для UI. */
export const OCR_STATUS_SHORT: Record<number, string> = {
  0: 'QUEUED', 1: 'IN_PROGRESS', 2: 'NEEDS_REVIEW', 3: 'UNREADABLE', 4: 'SUCCESS', 5: 'ERROR',
};

/** Цветовой вид статуса: gray ждёт/в работе, green успех, yellow на проверку, red нечитаемо/ошибка. */
export function ocrStatusKind(code: number): 'gray' | 'green' | 'yellow' | 'red' {
  if (code === 4) return 'green';
  if (code === 2) return 'yellow';
  if (code === 3 || code === 5) return 'red';
  return 'gray'; // 0 QUEUED, 1 IN_PROGRESS
}

/** Пользовательская подпись статуса (QUEUED — не ошибка, а ожидание). */
export function ocrUserLabel(code: number): string {
  switch (code) {
    case 4: return 'Распознано';
    case 2: return 'Требует проверки';
    case 3: return 'Не читается';
    case 5: return 'Ошибка распознавания';
    default: return 'Ожидает распознавания'; // 0, 1
  }
}
