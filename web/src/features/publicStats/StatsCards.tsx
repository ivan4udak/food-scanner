import type { PublicStats } from '@/api/stats';

const nf = new Intl.NumberFormat('ru-RU');

/** Верхние карточки публичной статистики. */
export function StatsCards({ stats }: { stats: PublicStats }) {
  const cards = [
    { label: 'Всего сканов', value: stats.totals.scans },
    { label: 'Товаров в каталоге', value: stats.totals.catalogEntries },
    { label: 'Загружено фото', value: stats.totals.photos },
    { label: 'Участников', value: stats.totals.contributors },
    { label: 'Сканов сегодня', value: stats.today.scans },
    { label: 'Новых товаров сегодня', value: stats.today.catalogEntries },
  ];
  return (
    <div className="stats-grid">
      {cards.map((c) => (
        <div className="stat-card" key={c.label}>
          <div className="stat-value">{nf.format(c.value)}</div>
          <div className="stat-label">{c.label}</div>
        </div>
      ))}
    </div>
  );
}
