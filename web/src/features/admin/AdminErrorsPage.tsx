import { useQuery } from '@tanstack/react-query';
import { adminErrors } from '@/api/admin';
import { LogTable } from '@/features/admin/LogTable';
import { time } from '@/features/admin/fmt';

/** Ошибки за сегодня: клиентские (WARN/ERROR) и серверные события. */
export function AdminErrorsPage() {
  const q = useQuery({ queryKey: ['admin-errors'], queryFn: () => adminErrors(200) });

  if (q.isLoading) return <p className="muted center">Загрузка…</p>;
  if (q.isError || !q.data) return <p className="error center">Не удалось загрузить.</p>;

  return (
    <div className="stack">
      <div className="card">
        <h2>Ошибки клиента ({q.data.client.length})</h2>
        <LogTable logs={q.data.client} />
      </div>
      <div className="card">
        <h2>Ошибки сервера ({q.data.server.length})</h2>
        {q.data.server.length === 0 ? <p className="muted center">Нет.</p> : (
          <div className="logview" style={{ maxHeight: 460 }}>
            {q.data.server.map((s) => (
              <div key={s.id} className={`logline lv-${s.level}`}>
                [{time(s.occurredAt)}] {s.level} {s.event} {s.path ?? ''}
                {s.httpStatus != null && ` (${s.httpStatus})`} {s.errorCode ?? ''} {s.errorMessage ?? ''}
                {s.exceptionClass && <div className="meta">{s.exceptionClass}</div>}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
