import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminUser, adminUserLogs, setUserRole } from '@/api/admin';
import { useAuthStore } from '@/store/authStore';
import { LogTable } from '@/features/admin/LogTable';
import { dt } from '@/features/admin/fmt';

const ROLES = ['USER', 'ADMIN', 'SUPER_ADMIN'];

/** Карточка пользователя: профиль, роль (для супер-админа), сессии, сканы, логи. */
export function AdminUserDetailPage() {
  const { id = '' } = useParams();
  const navigate = useNavigate();
  const isSuperAdmin = useAuthStore((s) => s.isSuperAdmin);
  const qc = useQueryClient();
  const user = useQuery({ queryKey: ['admin-user', id], queryFn: () => adminUser(id), refetchInterval: 20_000 });
  const logs = useQuery({ queryKey: ['admin-user-logs', id], queryFn: () => adminUserLogs(id, 200) });
  const [roleDraft, setRoleDraft] = useState<string>('');

  const roleMutation = useMutation({
    mutationFn: (role: string) => setUserRole(id, role),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-user', id] }),
  });

  if (user.isLoading) return <p className="muted center">Загрузка…</p>;
  if (user.isError || !user.data) return <p className="error center">Пользователь не найден.</p>;
  const { user: u, sessions, recentScans } = user.data;
  const selectedRole = roleDraft || u.role;

  return (
    <div className="stack">
      <div className="card">
        <h2><span className={`dot ${u.online ? 'online' : 'offline'}`} /> {u.username}</h2>
        <div className="row"><span>Роль</span><span className="value">{u.role}</span></div>
        {isSuperAdmin() && (
          <div className="field" style={{ marginTop: 8 }}>
            <label>Сменить роль (супер-админ)</label>
            <select value={selectedRole} onChange={(e) => setRoleDraft(e.target.value)}>
              {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
            </select>
            <button
              className="btn secondary"
              style={{ marginTop: 8 }}
              disabled={roleMutation.isPending || selectedRole === u.role}
              onClick={() => roleMutation.mutate(selectedRole)}
            >
              {roleMutation.isPending ? '…' : 'Применить роль'}
            </button>
            {roleMutation.isError && <span className="error">Не удалось сменить роль.</span>}
          </div>
        )}
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
