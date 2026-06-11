/** Статусы структурного извлечения (коды 0–5, см. docs/OCR.md) — подписи и цвет для UI. */
export const EXTRACTION_STATUS_SHORT: Record<number, string> = {
  0: 'QUEUED', 1: 'IN_PROGRESS', 2: 'STRUCTURED', 3: 'NEEDS_REVIEW', 4: 'FAILED', 5: 'SKIPPED',
};

/** Цветовой вид: green извлечено, yellow на проверку, red ошибка, gray ожидает/в работе/пропущено. */
export function extractionStatusKind(code: number): 'gray' | 'green' | 'yellow' | 'red' {
  if (code === 2) return 'green';
  if (code === 3) return 'yellow';
  if (code === 4) return 'red';
  return 'gray'; // 0 QUEUED, 1 IN_PROGRESS, 5 SKIPPED
}

/** Короткая подпись типа задачи. */
export const EXTRACTION_TYPE_SHORT: Record<string, string> = {
  TEXT_EXTRACTION: 'текст',
  IMAGE_FALLBACK_EXTRACTION: 'фото',
};

/** Requeue разрешён для NEEDS_REVIEW(3)/FAILED(4)/SKIPPED(5) — STRUCTURED(2) не трогаем без force. */
export const canRequeueExtraction = (code: number) => code === 3 || code === 4 || code === 5;
/** Skip разрешён для QUEUED(0)/NEEDS_REVIEW(3)/FAILED(4); IN_PROGRESS(1) — без действий. */
export const canSkipExtraction = (code: number) => code === 0 || code === 3 || code === 4;
/** Активные статусы (для live-refresh деталей): QUEUED/IN_PROGRESS. */
export const extractionIsActive = (code: number) => code === 0 || code === 1;
