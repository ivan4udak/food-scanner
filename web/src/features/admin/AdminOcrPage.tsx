import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { adminOcr, adminOcrSummary } from '@/api/admin';
import { dt } from '@/features/admin/fmt';

/** Короткие подписи статусов OCR (коды 0–5, см. docs/OCR.md). */
const STATUS_SHORT: Record<number, string> = {
  0: 'QUEUED', 1: 'IN_PROGRESS', 2: 'NEEDS_REVIEW', 3: 'UNREADABLE', 4: 'SUCCESS', 5: 'ERROR',
};

/** Страница наблюдаемости OCR: сводка по статусам + список задач с фильтрами. */
export function AdminOcrPage() {
  const [status, setStatus] = useState<number | undefined>(undefined);
  const [barcodeText, setBarcodeText] = useState('');
  const [barcode, setBarcode] = useState<string | undefined>(undefined);

  const summary = useQuery({ queryKey: ['admin-ocr-summary'], queryFn: adminOcrSummary });
  const jobs = useQuery({
    queryKey: ['admin-ocr', status, barcode],
    queryFn: () => adminOcr(status, barcode, 200),
  });

  return (
    <div className="card">
      <h2>OCR</h2>

      {/* Сводка по статусам — клик фильтрует список */}
      <nav className="chips tabs-row" style={{ flexWrap: 'wrap' }}>
        <button
          className={`chip ${status === undefined ? 'active' : ''}`}
          onClick={() => setStatus(undefined)}
        >
          Все{summary.data ? ` · ${summary.data.total}` : ''}
        </button>
        {summary.data?.byStatus.map((s) => (
          <button
            key={s.code}
            className={`chip ${status === s.code ? 'active' : ''}`}
            onClick={() => setStatus((cur) => (cur === s.code ? undefined : s.code))}
          >
            {STATUS_SHORT[s.code] ?? s.status} · {s.count}
          </button>
        ))}
      </nav>

      {/* Поиск по ШК */}
      <div className="field">
        <label>ШК</label>
        <input
          value={barcodeText}
          onChange={(e) => setBarcodeText(e.target.value)}
          placeholder="точный штрих-код…"
          inputMode="numeric"
          autoCapitalize="none"
        />
        <div className="chips" style={{ marginTop: 6 }}>
          <button className="chip" onClick={() => setBarcode(barcodeText.trim() || undefined)}>
            Найти
          </button>
          {barcode && (
            <button className="chip" onClick={() => { setBarcode(undefined); setBarcodeText(''); }}>
              Сброс
            </button>
          )}
        </div>
      </div>

      {jobs.isLoading && <p className="muted center">Загрузка…</p>}
      {jobs.data && jobs.data.length === 0 && <p className="muted center">Нет задач</p>}
      {jobs.data && jobs.data.length > 0 && (
        <div className="table-scroll">
          <table className="admin-table">
            <thead>
              <tr>
                <th>ШК</th><th>Тип</th><th>Статус</th><th>Попыт.</th>
                <th>Обновлено</th><th>Ошибка</th><th>Текст</th>
              </tr>
            </thead>
            <tbody>
              {jobs.data.map((j) => (
                <tr key={j.jobId}>
                  <td className="who">{j.barcode ?? '—'}</td>
                  <td className="sub">{j.photoType}</td>
                  <td className="sub">{STATUS_SHORT[j.statusCode] ?? j.status}</td>
                  <td className="num">{j.attempts}</td>
                  <td className="sub">{dt(j.updatedAt)}</td>
                  <td className="sub">{j.errorMessage ?? j.errorCode ?? '—'}</td>
                  <td className="sub" title={j.rawTextPreview ?? ''}>
                    {j.rawTextPreview ? j.rawTextPreview.slice(0, 40) : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
