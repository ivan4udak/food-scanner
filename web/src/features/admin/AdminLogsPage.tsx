import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { adminLogs, adminUsers, type LogFilters } from '@/api/admin';
import { LogTable } from '@/features/admin/LogTable';

const LEVELS = ['', 'ERROR', 'WARN', 'INFO', 'DEBUG'];
const CATEGORIES = ['', 'AUTH', 'API', 'NETWORK', 'SCAN', 'PHOTO', 'CATALOG', 'PWA', 'SYSTEM'];

/** Просмотр клиентских логов с фильтрами (включая выбор пользователя с автоподбором). */
export function AdminLogsPage() {
  const [filters, setFilters] = useState<LogFilters>({ limit: 200 });
  const [userText, setUserText] = useState('');
  const [applied, setApplied] = useState<LogFilters>({ limit: 200 });

  const users = useQuery({ queryKey: ['admin-users-mini'], queryFn: () => adminUsers('lastActivityAt', 500) });
  const logs = useQuery({ queryKey: ['admin-logs', applied], queryFn: () => adminLogs(applied) });

  const set = (patch: Partial<LogFilters>) => setFilters((f) => ({ ...f, ...patch }));

  function apply() {
    // Пользователь: по введённому нику находим contributorId (точное совпадение).
    const text = userText.trim().toLowerCase();
    const match = text ? users.data?.find((u) => u.username?.toLowerCase() === text) : undefined;
    setApplied({ ...filters, contributorId: match?.id });
  }

  function reset() {
    setFilters({ limit: 200 });
    setUserText('');
    setApplied({ limit: 200 });
  }

  return (
    <div className="card">
      <h2>Клиентские логи</h2>

      <div className="field">
        <label>Пользователь</label>
        <input
          list="admin-users-dl"
          value={userText}
          onChange={(e) => setUserText(e.target.value)}
          placeholder="начните вводить ник…"
          autoCapitalize="none"
        />
        <datalist id="admin-users-dl">
          {users.data?.map((u) => <option key={u.id} value={u.username} />)}
        </datalist>
        {userText.trim() && !users.data?.some((u) => u.username?.toLowerCase() === userText.trim().toLowerCase()) && (
          <span className="sub">нет точного совпадения — фильтр по пользователю не применится</span>
        )}
      </div>

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

      <div className="stack">
        <button className="btn secondary" onClick={apply}>Применить фильтр</button>
        <button className="btn ghost" onClick={reset}>Сбросить</button>
      </div>

      <div style={{ marginTop: 14 }}>
        {logs.isLoading ? <p className="muted center">Загрузка…</p> : <LogTable logs={logs.data ?? []} />}
      </div>
    </div>
  );
}
