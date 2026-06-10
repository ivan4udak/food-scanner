import { useQuery } from '@tanstack/react-query';
import { adminDashboard } from '@/api/admin';
import { LastUpdated } from '@/features/admin/LastUpdated';

const nf = new Intl.NumberFormat('ru-RU');

/** Admin Dashboard — операционная сводка (live-refresh без перезахода). */
export function AdminDashboardPage() {
  const q = useQuery({ queryKey: ['admin-dashboard'], queryFn: adminDashboard, refetchInterval: 15_000 });

  if (q.isLoading) return <p className="muted center">Загрузка…</p>;
  if (q.isError || !q.data) return <p className="error center">Не удалось загрузить дашборд.</p>;

  const d = q.data;
  const cards = [
    { label: 'Пользователей', value: d.usersTotal },
    { label: 'Онлайн сейчас', value: d.onlineNow },
    { label: 'Активны сегодня', value: d.activeToday },
    { label: 'Активны за неделю', value: d.activeWeek },
    { label: 'Сканов сегодня', value: d.scansToday },
    { label: 'Сканов за неделю', value: d.scansWeek },
    { label: 'Товаров сегодня', value: d.entriesToday },
    { label: 'Товаров за неделю', value: d.entriesWeek },
    { label: 'Фото сегодня', value: d.photosToday },
    { label: 'Ошибки клиента (сегодня)', value: d.clientErrorsToday },
    { label: 'Ошибки сервера (сегодня)', value: d.serverErrorsToday },
  ];

  return (
    <>
    <LastUpdated updatedAt={q.dataUpdatedAt} isFetching={q.isFetching} onRefresh={() => q.refetch()} />
    <div className="stats-grid">
      {cards.map((c) => (
        <div className="stat-card" key={c.label}>
          <div className="stat-value">{nf.format(c.value)}</div>
          <div className="stat-label">{c.label}</div>
        </div>
      ))}
    </div>
    </>
  );
}
