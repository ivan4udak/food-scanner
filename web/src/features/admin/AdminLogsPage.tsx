import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { adminLogs, type LogFilters } from '@/api/admin';
import { LogTable } from '@/features/admin/LogTable';

const LEVELS = ['', 'ERROR', 'WARN', 'INFO', 'DEBUG'];
const CATEGORIES = ['', 'AUTH', 'API', 'NETWORK', 'SCAN', 'PHOTO', 'CATALOG', 'PWA', 'SYSTEM'];

/** Просмотр клиентских логов с фильтрами. */
export function AdminLogsPage() {
  const [filters, setFilters] = useState<LogFilters>({ limit: 200 });
  const [applied, setApplied] = useState<LogFilters>({ limit: 200 });
  const q = useQuery({ queryKey: ['admin-logs', applied], queryFn: () => adminLogs(applied) });

  const set = (patch: Partial<LogFilters>) => setFilters((f) => ({ ...f, ...patch }));

  return (
    <div className="card">
      <h2>Клиентские логи</h2>
      <div className="field">
        <label>Уровень</label>
        <select value={filters.level ?? ''} onChange={(e) => set({ level: e.target.value || undefined })}>
          {LEVELS.map((l) => <option key={l} value={l}>{l || 'Все'}</option>)}
        </select>
      </div>
      <div className="field">
        <label>Категория</label>
        <select value={filters.category ?? ''} onChange={(e) => set({ category: e.target.value || undefined })}>
          {CATEGORIES.map((c) => <option key={c} value={c}>{c || 'Все'}</option>)}
        </select>
      </div>
      <div className="field">
        <label>Штрихкод</label>
        <input value={filters.barcode ?? ''} onChange={(e) => set({ barcode: e.target.value || undefined })} placeholder="необязательно" />
      </div>
      <div className="field">
        <label>Событие</label>
        <input value={filters.event ?? ''} onChange={(e) => set({ event: e.target.value || undefined })} placeholder="например SCAN_RESULT" />
      </div>
      <button className="btn secondary" onClick={() => setApplied({ ...filters })}>Применить фильтр</button>

      <div style={{ marginTop: 14 }}>
        {q.isLoading ? <p className="muted center">Загрузка…</p> : <LogTable logs={q.data ?? []} />}
      </div>
    </div>
  );
}
