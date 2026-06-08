import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getLeaderboard, getPublicStats, type LeaderboardPeriod } from '@/api/stats';
import { StatsCards } from '@/features/publicStats/StatsCards';
import { LeaderboardTable } from '@/features/publicStats/LeaderboardTable';
import { Page } from '@/components/Layout';

/** Публичная страница статистики проекта (без авторизации). */
export function PublicStatsPage() {
  const [period, setPeriod] = useState<LeaderboardPeriod>('all');

  const stats = useQuery({ queryKey: ['public-stats'], queryFn: getPublicStats });
  const board = useQuery({
    queryKey: ['leaderboard', period],
    queryFn: () => getLeaderboard(period, 50),
  });

  return (
    <Page>
      <div className="topbar">
        <h1 style={{ fontSize: '1.35rem', margin: 0 }}>Food Scanner — статистика</h1>
        <Link className="btn ghost" style={{ width: 'auto' }} to="/">
          В приложение
        </Link>
      </div>

      {stats.isLoading && <p className="muted center">Загрузка статистики…</p>}
      {stats.isError && <p className="error center">Не удалось загрузить статистику.</p>}
      {stats.data && <StatsCards stats={stats.data} />}

      <LeaderboardTable
        board={board.data}
        period={period}
        onPeriod={setPeriod}
        loading={board.isLoading}
      />

      <p className="muted center" style={{ fontSize: '0.8rem' }}>
        Рейтинг строится по числу завершённых товаров (затем — фото, затем — сканы).
      </p>
    </Page>
  );
}
