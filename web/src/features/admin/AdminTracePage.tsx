import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { adminTrace } from '@/api/admin';
import { time } from '@/features/admin/fmt';

/** Сквозная трассировка по correlationId: client_logs + server_events в одной линии. */
export function AdminTracePage() {
  const { correlationId = '' } = useParams();
  const q = useQuery({ queryKey: ['admin-trace', correlationId], queryFn: () => adminTrace(correlationId) });

  return (
    <div className="card">
      <h2>Трассировка</h2>
      <p className="muted" style={{ fontSize: '0.78rem', wordBreak: 'break-all' }}>correlationId: {correlationId}</p>

      {q.isLoading && <p className="muted center">Загрузка…</p>}
      {q.data && q.data.length === 0 && <p className="muted center">Событий не найдено.</p>}
      {q.data && q.data.length > 0 && (
        <div className="logview" style={{ maxHeight: 560 }}>
          {q.data.map((t, i) => (
            <div key={i} className={`logline lv-${t.level ?? 'INFO'}`}>
              [{time(t.at)}] <b>{t.source}</b> {t.event ?? ''} {t.message ?? ''}
              {t.method && ` ${t.method} ${t.path ?? ''}`}
              {t.httpStatus != null && ` (${t.httpStatus})`}
              {t.durationMs != null && ` ${t.durationMs}ms`}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
