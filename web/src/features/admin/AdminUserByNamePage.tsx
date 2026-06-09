import { Navigate, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { adminUserByName } from '@/api/admin';

/** Резолвит ник → редирект на карточку пользователя (переход из /stats). */
export function AdminUserByNamePage() {
  const { username = '' } = useParams();
  const q = useQuery({ queryKey: ['admin-user-by-name', username], queryFn: () => adminUserByName(username) });

  if (q.isLoading) return <p className="muted center">Поиск пользователя…</p>;
  if (q.isError || !q.data) return <p className="error center">Пользователь не найден.</p>;
  return <Navigate to={`/admin/users/${q.data.user.id}`} replace />;
}
