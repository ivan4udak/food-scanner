import { NavLink, Navigate, Outlet, useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';
import { Page } from '@/components/Layout';

const TABS = [
  { to: '/admin', label: 'Дашборд', end: true },
  { to: '/admin/users', label: 'Пользователи' },
  { to: '/admin/logs', label: 'Логи' },
  { to: '/admin/catalog', label: 'Каталог' },
  { to: '/admin/errors', label: 'Ошибки' },
];

/** Каркас админ-панели: гард доступа (только ADMIN) + навигация. */
export function AdminLayout() {
  const isAdmin = useAuthStore((s) => s.isAdmin);
  const navigate = useNavigate();

  if (!isAdmin()) return <Navigate to="/" replace />;

  const goBack = () => (window.history.length > 1 ? navigate(-1) : navigate('/'));

  return (
    <Page>
      <div className="topbar">
        <h1 style={{ fontSize: '1.3rem', margin: 0 }}>Админ-панель</h1>
        <button className="btn ghost" style={{ width: 'auto' }} onClick={goBack}>
          ‹ Назад
        </button>
      </div>

      <nav className="chips tabs-row">
        {TABS.map((t) => (
          <NavLink
            key={t.to}
            to={t.to}
            end={t.end}
            className={({ isActive }) => `chip ${isActive ? 'active' : ''}`}
          >
            {t.label}
          </NavLink>
        ))}
      </nav>

      <Outlet />
    </Page>
  );
}
