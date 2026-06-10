import { time } from '@/features/admin/fmt';

/** Индикатор «Обновлено: HH:mm:ss» + кнопка ручного обновления (live-refresh страниц). */
export function LastUpdated({ updatedAt, isFetching, onRefresh }: {
  updatedAt: number;
  isFetching?: boolean;
  onRefresh?: () => void;
}) {
  return (
    <div className="sub" style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
      <span>
        Обновлено: {updatedAt ? time(new Date(updatedAt).toISOString()) : '—'}{isFetching ? ' · …' : ''}
      </span>
      {onRefresh && (
        <button className="chip" onClick={onRefresh} disabled={isFetching}>Обновить</button>
      )}
    </div>
  );
}
