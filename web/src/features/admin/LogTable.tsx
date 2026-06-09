import { useNavigate } from 'react-router-dom';
import type { AdminClientLog } from '@/api/admin';
import { time } from '@/features/admin/fmt';

/** Таблица клиентских логов. Клик по строке с correlationId → сквозная трассировка. */
export function LogTable({ logs }: { logs: AdminClientLog[] }) {
  const navigate = useNavigate();
  if (logs.length === 0) return <p className="muted center">Логов нет.</p>;
  return (
    <div className="logview" style={{ maxHeight: 460 }}>
      {logs.map((l) => (
        <div
          key={l.id}
          className={`logline lv-${l.level} ${l.correlationId ? 'clickable' : ''}`}
          onClick={() => l.correlationId && navigate(`/admin/trace/${l.correlationId}`)}
          title={l.correlationId ? 'Открыть трассировку' : undefined}
        >
          [{time(l.timestamp)}] {l.level} {l.category} {l.event ?? ''} {l.message ?? ''}
          {l.httpStatus != null && ` (${l.httpStatus})`}
          {l.metadataJson && <div className="meta">{l.metadataJson}</div>}
        </div>
      ))}
    </div>
  );
}
