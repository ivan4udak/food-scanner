import type { Leaderboard, LeaderboardPeriod } from '@/api/stats';

const PERIODS: { id: LeaderboardPeriod; label: string }[] = [
  { id: 'all', label: 'Всё время' },
  { id: 'month', label: 'Месяц' },
  { id: 'week', label: 'Неделя' },
  { id: 'today', label: 'Сегодня' },
];

interface Props {
  board?: Leaderboard;
  period: LeaderboardPeriod;
  onPeriod: (p: LeaderboardPeriod) => void;
  loading?: boolean;
}

/** Таблица рейтинга участников + переключатель периода. */
export function LeaderboardTable({ board, period, onPeriod, loading }: Props) {
  return (
    <div className="card">
      <h2>Рейтинг участников</h2>
      <div className="chips">
        {PERIODS.map((p) => (
          <button
            key={p.id}
            className={`chip ${p.id === period ? 'active' : ''}`}
            onClick={() => onPeriod(p.id)}
          >
            {p.label}
          </button>
        ))}
      </div>

      {loading && <p className="muted center">Загрузка…</p>}
      {!loading && board && board.items.length === 0 && (
        <p className="muted center">Пока нет данных за этот период.</p>
      )}

      {board && board.items.length > 0 && (
        <table className="leaderboard">
          <thead>
            <tr>
              <th>#</th>
              <th>Участник</th>
              <th title="Завершённых товаров">Товаров</th>
              <th title="Загружено фото">Фото</th>
              <th title="Сканов">Сканов</th>
            </tr>
          </thead>
          <tbody>
            {board.items.map((it) => (
              <tr key={it.rank}>
                <td className="rank">{it.rank}</td>
                <td className="who">{it.username}</td>
                <td className="num strong">{it.completedEntries}</td>
                <td className="num">{it.uploadedPhotos}</td>
                <td className="num">{it.scans}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
