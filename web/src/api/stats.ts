import { api } from '@/api/client';

export interface PublicStats {
  totals: { scans: number; catalogEntries: number; photos: number; contributors: number };
  today: { scans: number; catalogEntries: number; photos: number };
}

export interface LeaderboardItem {
  rank: number;
  username: string;
  completedEntries: number;
  scans: number;
  uploadedPhotos: number;
  score: number;
}

export interface Leaderboard {
  period: string;
  items: LeaderboardItem[];
}

export type LeaderboardPeriod = 'all' | 'today' | 'week' | 'month';

/** GET /public/stats — публично. */
export async function getPublicStats(): Promise<PublicStats> {
  const res = await api.get('/public/stats');
  return res.data as PublicStats;
}

/** GET /public/leaderboard — публично. */
export async function getLeaderboard(period: LeaderboardPeriod = 'all', limit = 50): Promise<Leaderboard> {
  const res = await api.get('/public/leaderboard', { params: { period, limit } });
  return res.data as Leaderboard;
}

/** POST /me/leaderboard-visibility — требует авторизации. → актуальное «скрыт ли». */
export async function setLeaderboardVisibility(hidden: boolean): Promise<boolean> {
  const res = await api.post('/me/leaderboard-visibility', { hidden });
  return Boolean(res.data?.hiddenFromLeaderboard);
}
