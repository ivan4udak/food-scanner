import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { adminUsers } from '@/api/admin';
import { dt } from '@/features/admin/fmt';

const SORTS = [
  { id: 'lastActivityAt', label: 'Активность' },
  { id: 'completedEntries', label: 'Товаров' },
  { id: 'uploadedPhotos', label: 'Фото' },
  { id: 'totalScans', label: 'Сканов' },
  { id: 'clientErrors', label: 'Ошибок' },
];

/** Список пользователей с метриками и фильтром сортировки. */
export function AdminUsersPage() {
  const navigate = useNavigate();
  const [sort, setSort] = useState('lastActivityAt');
  const q = useQuery({
    queryKey: ['admin-users', sort],
    queryFn: () => adminUsers(sort, 200),
    refetchInterval: 20_000, // присутствие обновляется без перезагрузки
  });

  return (
    <div className="card">
      <div className="chips">
        {SORTS.map((s) => (
          <button key={s.id} className={`chip ${s.id === sort ? 'active' : ''}`} onClick={() => setSort(s.id)}>
            {s.label}
          </button>
        ))}
      </div>

      {q.isLoading && <p className="muted center">Загрузка…</p>}
      {q.data && (
        <table className="admin-table">
          <thead>
            <tr><th>Пользователь</th><th>Активность</th><th title="Товаров">Тов.</th>
              <th title="Фото">Фото</th><th title="Сканов">Скан.</th><th title="Ошибок">Ош.</th></tr>
          </thead>
          <tbody>
            {q.data.map((u) => (
              <tr key={u.id} className="clickable" onClick={() => navigate(`/admin/users/${u.id}`)}>
                <td className="who">
                  <div className="who-line">
                    <span className={`dot ${u.online ? 'online' : 'offline'}`} />
                    <span className="uname">{u.username}</span>
                    {u.role !== 'USER' && <span className="badge-role">{u.role}</span>}
                  </div>
                  <div className="sub">{u.os ?? '—'} · {u.browser ?? '—'} · {u.clientVersion ?? '—'}</div>
                </td>
                <td className="sub">{dt(u.lastActivityAt)}</td>
                <td className="num strong">{u.completedEntries}</td>
                <td className="num">{u.uploadedPhotos}</td>
                <td className="num">{u.totalScans}</td>
                <td className="num" style={{ color: u.clientErrors ? 'var(--danger)' : undefined }}>{u.clientErrors}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
