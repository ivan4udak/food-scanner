import { useNavigate } from 'react-router-dom';
import type { AdminOcrRow } from '@/api/admin';
import { dt } from '@/features/admin/fmt';
import { OCR_STATUS_SHORT, ocrStatusKind } from '@/features/admin/ocr';

const KIND_COLOR: Record<string, string> = {
  gray: '#9aa0a6', green: '#34a853', yellow: '#f9ab00', red: '#ea4335',
};
function StatusDot({ code }: { code: number }) {
  return <span style={{
    display: 'inline-block', width: 8, height: 8, borderRadius: '50%',
    background: KIND_COLOR[ocrStatusKind(code)], marginRight: 6,
  }} />;
}

/** Таблица OCR-задач (переиспользуется в /admin/ocr, карточках каталога и пользователя). */
export function OcrJobsTable({ jobs, showBarcode = true, showUser = false }: {
  jobs: AdminOcrRow[];
  showBarcode?: boolean;
  showUser?: boolean;
}) {
  const navigate = useNavigate();
  if (!jobs || jobs.length === 0) return <p className="muted">Нет OCR-задач.</p>;

  return (
    <div className="table-scroll">
      <table className="admin-table">
        <thead>
          <tr>
            {showBarcode && <th>ШК</th>}
            {showUser && <th>Автор</th>}
            <th>Тип</th><th>Статус</th><th></th><th>Попыт.</th><th>Обновлено</th><th>Текст / ошибка</th>
          </tr>
        </thead>
        <tbody>
          {jobs.map((j) => (
            <tr key={j.jobId}>
              {showBarcode && (
                <td className="who clickable"
                    onClick={() => j.barcode && navigate(`/admin/catalog/${encodeURIComponent(j.barcode)}`)}>
                  {j.barcode ?? '—'}
                </td>
              )}
              {showUser && (
                <td className="sub clickable"
                    onClick={() => j.contributorId && navigate(`/admin/users/${j.contributorId}`)}>
                  {j.author ?? '—'}
                </td>
              )}
              <td className="sub">{j.photoType}</td>
              <td className="sub">
                <StatusDot code={j.statusCode} />{OCR_STATUS_SHORT[j.statusCode] ?? j.status}
              </td>
              <td className="sub">{!j.active ? (j.orphaned ? 'orphaned' : 'inactive') : ''}</td>
              <td className="num">{j.attempts}</td>
              <td className="sub">{dt(j.updatedAt)}</td>
              <td className="sub" title={j.rawTextPreview ?? j.errorMessage ?? ''}>
                {j.errorMessage ?? j.errorCode ?? (j.rawTextPreview ? j.rawTextPreview.slice(0, 30) : '—')}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
