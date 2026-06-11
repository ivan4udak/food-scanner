import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  adminExtractionDetail, adminRequeueExtraction, adminSkipExtraction,
} from '@/api/admin';
import { dt } from '@/features/admin/fmt';
import {
  EXTRACTION_STATUS_SHORT, EXTRACTION_TYPE_SHORT, extractionStatusKind,
  canRequeueExtraction, canSkipExtraction, extractionIsActive,
} from '@/features/admin/extraction';
import { OCR_STATUS_SHORT } from '@/features/admin/ocr';

const KIND_COLOR: Record<string, string> = { gray: '#9aa0a6', green: '#34a853', yellow: '#f9ab00', red: '#ea4335' };

/** Полная карточка задачи извлечения: статус, связи, структурный результат, OCR-источник, действия. */
export function AdminExtractionDetailPage() {
  const { jobId = '' } = useParams();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [copied, setCopied] = useState(false);

  const q = useQuery({
    queryKey: ['admin-extraction-detail', jobId],
    queryFn: () => adminExtractionDetail(jobId),
    refetchInterval: (query) => (extractionIsActive(query.state.data?.statusCode ?? -1) ? 5000 : false),
  });
  const requeue = useMutation({
    mutationFn: () => adminRequeueExtraction(jobId),
    onSuccess: (newId) => { qc.invalidateQueries(); if (newId) navigate(`/admin/extraction/${newId}`); },
  });
  const skip = useMutation({
    mutationFn: () => adminSkipExtraction(jobId),
    onSuccess: () => qc.invalidateQueries(),
  });

  if (q.isLoading) return <p className="muted center">Загрузка…</p>;
  if (q.isError || !q.data) return <p className="error center">Задача не найдена.</p>;
  const j = q.data;
  const busy = requeue.isPending || skip.isPending;

  const copyRaw = async () => {
    if (!j.ocr?.rawText) return;
    try {
      await navigator.clipboard.writeText(j.ocr.rawText);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch { /* ignore */ }
  };

  const hasStructured = j.name || j.brand || j.manufacturer || j.composition || j.nutrition;

  return (
    <div className="stack">
      <div className="card">
        <h2>
          <span style={{ display: 'inline-block', width: 10, height: 10, borderRadius: '50%',
            background: KIND_COLOR[extractionStatusKind(j.statusCode)], marginRight: 8 }} />
          {EXTRACTION_STATUS_SHORT[j.statusCode] ?? j.status} · {EXTRACTION_TYPE_SHORT[j.type] ?? j.type}
        </h2>
        <div className="row"><span>ШК</span><span className="value">
          {j.barcode
            ? <button className="chip" onClick={() => navigate(`/admin/catalog/${encodeURIComponent(j.barcode!)}`)}>{j.barcode}</button>
            : '—'}
        </span></div>
        <div className="row"><span>OCR-источник</span><span className="value">
          <button className="chip" onClick={() => navigate(`/admin/ocr/${j.ocrJobId}`)}>{j.ocrJobId}</button>
        </span></div>
        <div className="row"><span>Источник (source)</span><span className="value">{j.source ?? '—'}</span></div>
        <div className="row"><span>Попытки</span><span className="value">{j.attempts}</span></div>
        <div className="row"><span>needsReview</span><span className="value">{j.needsReview === null ? '—' : String(j.needsReview)}</span></div>
        <div className="row"><span>queued / started</span><span className="value">{dt(j.queuedAt)} · {dt(j.startedAt)}</span></div>
        <div className="row"><span>processed / updated</span><span className="value">{dt(j.processedAt)} · {dt(j.updatedAt)}</span></div>
        {j.lastError && <div className="row"><span>lastError</span><span className="value">{j.lastError}</span></div>}

        <div className="chips" style={{ marginTop: 10 }}>
          {canRequeueExtraction(j.statusCode) && (
            <button className="btn secondary" style={{ width: 'auto' }} disabled={busy} onClick={() => requeue.mutate()}>
              ↻ Переотправить
            </button>
          )}
          {canSkipExtraction(j.statusCode) && (
            <button className="btn ghost" style={{ width: 'auto' }} disabled={busy} onClick={() => skip.mutate()}>
              ⤬ Пропустить
            </button>
          )}
        </div>
      </div>

      <div className="card">
        <h2>Структурный результат</h2>
        {hasStructured
          ? (<>
              {j.name && <div className="row"><span>Название</span><span className="value">{j.name}</span></div>}
              {j.brand && <div className="row"><span>Бренд</span><span className="value">{j.brand}</span></div>}
              {j.manufacturer && <div className="row"><span>Производитель</span><span className="value">{j.manufacturer}</span></div>}
              {j.composition && <div className="row"><span>Состав</span><span className="value">{j.composition}</span></div>}
              {j.nutrition && <div className="row"><span>КБЖУ</span><span className="value">{j.nutrition}</span></div>}
              {j.confidence && <div className="row"><span>confidence</span><span className="value">{j.confidence}</span></div>}
            </>)
          : <p className="muted">Пока не извлечено (реальный LLM/Vision — будущий срез).</p>}
      </div>

      <div className="card">
        <h2>OCR-источник</h2>
        {j.ocr
          ? (<>
              <div className="row"><span>Статус</span><span className="value">
                {OCR_STATUS_SHORT[j.ocr.statusCode] ?? j.ocr.status} · {j.ocr.photoType}
              </span></div>
              <div className="row"><span>Confidence</span><span className="value">{j.ocr.confidence ?? '—'}</span></div>
              <div className="row"><span>Длина текста</span><span className="value">{j.ocr.rawTextLength}</span></div>
              {(j.ocr.errorCode || j.ocr.errorMessage) && (
                <div className="row"><span>Ошибка</span><span className="value">{j.ocr.errorCode ?? ''} {j.ocr.errorMessage ?? ''}</span></div>
              )}
              {j.ocr.rawText
                ? (<>
                    <button className="btn ghost" style={{ width: 'auto' }} onClick={copyRaw}>
                      {copied ? 'Скопировано ✓' : 'Скопировать rawText'}
                    </button>
                    <pre className="logview" style={{ whiteSpace: 'pre-wrap' }}>{j.ocr.rawText}</pre>
                  </>)
                : <p className="muted">Нет текста.</p>}
            </>)
          : <p className="muted">OCR-задача-источник не найдена.</p>}
      </div>
    </div>
  );
}
