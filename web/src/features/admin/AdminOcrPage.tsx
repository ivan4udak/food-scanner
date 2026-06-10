import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { adminOcr, adminOcrSummary } from '@/api/admin';
import { OcrJobsTable } from '@/features/admin/OcrJobsTable';
import { OCR_STATUS_SHORT } from '@/features/admin/ocr';

/** Страница наблюдаемости OCR: сводка по статусам + список задач с фильтрами. */
export function AdminOcrPage() {
  const [status, setStatus] = useState<number | undefined>(undefined);
  const [barcodeText, setBarcodeText] = useState('');
  const [barcode, setBarcode] = useState<string | undefined>(undefined);
  const [showInactive, setShowInactive] = useState(false);
  const [showOrphaned, setShowOrphaned] = useState(false);

  const summary = useQuery({
    queryKey: ['admin-ocr-summary'], queryFn: adminOcrSummary, refetchInterval: 10_000,
  });
  const jobs = useQuery({
    queryKey: ['admin-ocr', status, barcode, showInactive, showOrphaned],
    queryFn: () => adminOcr(status, barcode, showInactive, showOrphaned, 200),
    refetchInterval: 10_000,
  });

  const oldest = summary.data?.oldestQueuedAgeSeconds ?? 0;
  const queueWarn = oldest > 300; // старейшая QUEUED висит > 5 мин — возможна перегрузка

  return (
    <div className="card">
      <h2>OCR</h2>

      {summary.data && (
        <p className="sub" style={{ margin: '0 0 8px' }}>
          Очередь: {summary.data.queueSize} · старейшая ждёт {Math.round(oldest)}s
          {queueWarn && <span className="error"> · ⚠ очередь растёт — worker может быть перегружен</span>}
        </p>
      )}

      {/* Сводка по статусам — клик фильтрует список */}
      <nav className="chips tabs-row" style={{ flexWrap: 'wrap' }}>
        <button className={`chip ${status === undefined ? 'active' : ''}`} onClick={() => setStatus(undefined)}>
          Все{summary.data ? ` · ${summary.data.total}` : ''}
        </button>
        {summary.data?.byStatus.map((s) => (
          <button key={s.code} className={`chip ${status === s.code ? 'active' : ''}`}
                  onClick={() => setStatus((cur) => (cur === s.code ? undefined : s.code))}>
            {OCR_STATUS_SHORT[s.code] ?? s.status} · {s.count}
          </button>
        ))}
      </nav>

      {/* Поиск по ШК + фильтры видимости */}
      <div className="field">
        <label>ШК</label>
        <input value={barcodeText} onChange={(e) => setBarcodeText(e.target.value)}
               placeholder="точный штрих-код…" inputMode="numeric" autoCapitalize="none" />
        <div className="chips" style={{ marginTop: 6 }}>
          <button className="chip" onClick={() => setBarcode(barcodeText.trim() || undefined)}>Найти</button>
          {barcode && (
            <button className="chip" onClick={() => { setBarcode(undefined); setBarcodeText(''); }}>Сброс</button>
          )}
          <button className={`chip ${showInactive ? 'active' : ''}`} onClick={() => setShowInactive((v) => !v)}>
            inactive
          </button>
          <button className={`chip ${showOrphaned ? 'active' : ''}`} onClick={() => setShowOrphaned((v) => !v)}>
            orphaned
          </button>
        </div>
      </div>

      {jobs.isLoading && <p className="muted center">Загрузка…</p>}
      {jobs.data && <OcrJobsTable jobs={jobs.data} showUser />}
    </div>
  );
}
