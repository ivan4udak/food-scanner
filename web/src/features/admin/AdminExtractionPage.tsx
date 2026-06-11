import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { adminExtraction, adminExtractionSummary, type AdminExtractionRow } from '@/api/admin';
import { dt } from '@/features/admin/fmt';
import { LastUpdated } from '@/features/admin/LastUpdated';
import { EXTRACTION_STATUS_SHORT, EXTRACTION_TYPE_SHORT, extractionStatusKind } from '@/features/admin/extraction';

const KIND_COLOR: Record<string, string> = {
  gray: '#9aa0a6', green: '#34a853', yellow: '#f9ab00', red: '#ea4335',
};
function StatusDot({ code }: { code: number }) {
  return <span style={{
    display: 'inline-block', width: 8, height: 8, borderRadius: '50%',
    background: KIND_COLOR[extractionStatusKind(code)], marginRight: 6,
  }} />;
}

const TYPES = ['TEXT_EXTRACTION', 'IMAGE_FALLBACK_EXTRACTION'] as const;

/** Наблюдаемость структурного извлечения: сводка по статусам + список задач с фильтрами. */
export function AdminExtractionPage() {
  const navigate = useNavigate();
  const [status, setStatus] = useState<number | undefined>(undefined);
  const [type, setType] = useState<string | undefined>(undefined);

  const summary = useQuery({
    queryKey: ['admin-extraction-summary'], queryFn: adminExtractionSummary, refetchInterval: 10_000,
  });
  const jobs = useQuery({
    queryKey: ['admin-extraction', status, type],
    queryFn: () => adminExtraction(status, type, undefined, 200),
    refetchInterval: 10_000,
  });

  return (
    <div className="card">
      <h2>Извлечение</h2>
      <LastUpdated updatedAt={jobs.dataUpdatedAt} isFetching={jobs.isFetching}
                   onRefresh={() => { jobs.refetch(); summary.refetch(); }} />

      {/* Сводка по статусам — клик фильтрует список */}
      <nav className="chips tabs-row" style={{ flexWrap: 'wrap' }}>
        <button className={`chip ${status === undefined ? 'active' : ''}`} onClick={() => setStatus(undefined)}>
          Все{summary.data ? ` · ${summary.data.total}` : ''}
        </button>
        {summary.data?.byStatus.map((s) => (
          <button key={s.code} className={`chip ${status === s.code ? 'active' : ''}`}
                  onClick={() => setStatus((cur) => (cur === s.code ? undefined : s.code))}>
            {EXTRACTION_STATUS_SHORT[s.code] ?? s.status} · {s.count}
          </button>
        ))}
      </nav>

      {/* Фильтр по типу источника */}
      <div className="chips" style={{ marginTop: 6 }}>
        <button className={`chip ${type === undefined ? 'active' : ''}`} onClick={() => setType(undefined)}>
          все типы
        </button>
        {TYPES.map((t) => (
          <button key={t} className={`chip ${type === t ? 'active' : ''}`}
                  onClick={() => setType((cur) => (cur === t ? undefined : t))}>
            {EXTRACTION_TYPE_SHORT[t] ?? t}
          </button>
        ))}
      </div>

      {jobs.isLoading && <p className="muted center">Загрузка…</p>}
      {jobs.data && <ExtractionJobsTable jobs={jobs.data}
                                         onOpenDetail={(id) => navigate(`/admin/extraction/${id}`)}
                                         onOpenOcr={(id) => navigate(`/admin/ocr/${id}`)}
                                         onOpenCatalog={(bc) => navigate(`/admin/catalog/${encodeURIComponent(bc)}`)} />}
    </div>
  );
}

function ExtractionJobsTable({ jobs, onOpenDetail, onOpenOcr, onOpenCatalog }: {
  jobs: AdminExtractionRow[];
  onOpenDetail: (jobId: string) => void;
  onOpenOcr: (ocrJobId: string) => void;
  onOpenCatalog: (barcode: string) => void;
}) {
  if (!jobs || jobs.length === 0) return <p className="muted">Нет задач извлечения.</p>;
  return (
    <div className="table-scroll">
      <table className="admin-table">
        <thead>
          <tr>
            <th>ШК</th><th>Тип</th><th>Статус</th><th>Попыт.</th><th>Поля</th>
            <th>Источник</th><th>Обновлено</th><th>Ошибка</th><th></th>
          </tr>
        </thead>
        <tbody>
          {jobs.map((j) => {
            const fields = [j.name, j.brand, j.manufacturer].filter(Boolean).join(' · ');
            return (
              <tr key={j.jobId}>
                <td className="who clickable" onClick={() => j.barcode && onOpenCatalog(j.barcode)}>
                  {j.barcode ?? '—'}
                </td>
                <td className="sub">{EXTRACTION_TYPE_SHORT[j.type] ?? j.type}</td>
                <td className="sub">
                  <StatusDot code={j.statusCode} />{EXTRACTION_STATUS_SHORT[j.statusCode] ?? j.status}
                </td>
                <td className="num">{j.attempts}</td>
                <td className="sub" title={fields}>{fields ? fields.slice(0, 40) : '—'}</td>
                <td className="sub">{j.source ?? '—'}</td>
                <td className="sub">{dt(j.updatedAt)}</td>
                <td className="sub" title={j.lastError ?? ''}>{j.lastError ? j.lastError.slice(0, 30) : '—'}</td>
                <td>
                  <button className="chip" title="Детали задачи"
                          onClick={() => onOpenDetail(j.jobId)}>детали</button>
                  <button className="chip" title="OCR-задача источника"
                          onClick={() => onOpenOcr(j.ocrJobId)}>OCR</button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
