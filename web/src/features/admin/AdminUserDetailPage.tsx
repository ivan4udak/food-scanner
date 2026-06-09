import { useNavigate, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { adminUser, adminUserLogs } from '@/api/admin';
import { LogTable } from '@/features/admin/LogTable';
import { dt } from '@/features/admin/fmt';

/** Карточка пользователя: профиль, сессии, сканы, логи. */
export function AdminUserDetailPage() {
  const { id = '' } = useParams();
  const navigate = useNavigate();
  const user = useQuery({ queryKey: ['admin-user', id], queryFn: () => adminUser(id), refetchInterval: 20_000 });
  const logs = useQuery({ queryKey: ['admin-user-logs', id], queryFn: () => adminUserLogs(id, 200) });

  if (user.isLoading) return <p className="muted center">Загрузка…</p>;
  if (user.isError || !user.data) return <p className="error center">Пользователь не найден.</p>;
  const { user: u, sessions, recentScans } = user.data;

  return (
    <div className="stack">
      <div className="card">
        <h2><span className={`dot ${u.online ? 'online' : 'offline'}`} /> {u.username}</h2>
        <div className="row"><span>Роль</span><span className="value">{u.role}</span></div>
        <div className="row"><span>Последняя активность</span><span className="value">{dt(u.lastActivityAt)}</span></div>
        <div className="row"><span>Устройство</span><span className="value">{u.os} · {u.browser} · {u.deviceType}</span></div>
        <div className="row"><span>Версия клиента</span><span className="value">{u.clientVersion ?? '—'}</span></div>
        <div className="row"><span>Товаров / Фото / Сканов</span><span className="value">{u.completedEntries} / {u.uploadedPhotos} / {u.totalScans}</span></div>
        <div
          className="row"
          style={{ cursor: 'pointer' }}
          onClick={() => navigate(`/admin/users/${id}/errors`)}
        >
          <span>Ошибок клиента</span><span className="value">{u.clientErrors}</span>
        </div>
      </div>

      <div className="card">
        <h2>Сканы</h2>
        {recentScans.length === 0 ? <p className="muted">Нет.</p> : (
          <div className="table-scroll">
          <table className="admin-table">
            <thead><tr><th>ШК</th><th>Статус</th><th>Фото</th><th>Когда</th></tr></thead>
            <tbody>
              {recentScans.map((s) => (
                <tr key={s.barcode} className="clickable" onClick={() => navigate(`/admin/catalog/${encodeURIComponent(s.barcode)}`)}>
                  <td className="who">{s.barcode}</td>
                  <td className="sub">{s.status}</td>
                  <td className="num">{s.photoCount}</td>
                  <td className="sub">{dt(s.firstScannedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          </div>
        )}
      </div>

      <div className="card">
        <h2>Сессии</h2>
        {sessions.length === 0 ? <p className="muted">Нет.</p> : (
          <div className="table-scroll">
          <table className="admin-table">
            <thead><tr><th>Начало</th><th>Активность</th><th>Устройство</th></tr></thead>
            <tbody>
              {sessions.map((s) => (
                <tr key={s.sessionId}>
                  <td className="sub">{dt(s.startedAt)}</td>
                  <td className="sub">{dt(s.lastSeenAt)}</td>
                  <td className="sub">{s.os} · {s.browser}{s.standalone ? ' · PWA' : ''}</td>
                </tr>
              ))}
            </tbody>
          </table>
          </div>
        )}
      </div>

      <div className="card">
        <h2>Клиентские логи</h2>
        {logs.isLoading ? <p className="muted">Загрузка…</p> : <LogTable logs={logs.data ?? []} />}
      </div>
    </div>
  );
}
