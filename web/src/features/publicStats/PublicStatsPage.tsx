import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getLeaderboard, getPublicStats, type LeaderboardPeriod } from '@/api/stats';
import { StatsCards } from '@/features/publicStats/StatsCards';
import { LeaderboardTable } from '@/features/publicStats/LeaderboardTable';
import { Page } from '@/components/Layout';
import { useAuthStore } from '@/store/authStore';

/** Публичная страница статистики проекта (без авторизации). */
export function PublicStatsPage() {
  const navigate = useNavigate();
  const isAdmin = useAuthStore((s) => s.isAdmin);
  const [period, setPeriod] = useState<LeaderboardPeriod>('all');

  /** Назад на страницу, откуда зашли; если истории нет (прямой переход/новая вкладка) — на главную. */
  function goBack() {
    if (window.history.length > 1) navigate(-1);
    else navigate('/');
  }

  const stats = useQuery({ queryKey: ['public-stats'], queryFn: getPublicStats });
  const board = useQuery({
    queryKey: ['leaderboard', period],
    queryFn: () => getLeaderboard(period, 50),
  });

  return (
    <Page>
      <div className="topbar">
        <h1 style={{ fontSize: '1.35rem', margin: 0 }}>Food Scanner — статистика</h1>
        <button className="btn ghost" style={{ width: 'auto' }} onClick={goBack}>
          ‹ Назад
        </button>
      </div>

      {stats.isLoading && <p className="muted center">Загрузка статистики…</p>}
      {stats.isError && <p className="error center">Не удалось загрузить статистику.</p>}
      {stats.data && <StatsCards stats={stats.data} />}

      <LeaderboardTable
        board={board.data}
        period={period}
        onPeriod={setPeriod}
        loading={board.isLoading}
        onUser={isAdmin() ? (u) => navigate(`/admin/u/${encodeURIComponent(u)}`) : undefined}
      />
      {isAdmin() && (
        <p className="muted center" style={{ fontSize: '0.78rem' }}>
          Вы админ: нажмите на ник, чтобы открыть его сканы и фото.
        </p>
      )}

      <p className="muted center" style={{ fontSize: '0.8rem' }}>
        Рейтинг строится по числу завершённых товаров (затем — фото, затем — сканы).
      </p>
    </Page>
  );
}
