import { NavLink, Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom';
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
  const { pathname } = useLocation();

  if (!isAdmin()) return <Navigate to="/" replace />;

  /**
   * Иерархический «Назад»:
   *  - карточка пользователя (/admin/users/:id, /admin/u/:name) → список «Пользователи»;
   *  - карточка каталога (/admin/catalog/:barcode) → список «Каталог»;
   *  - трассировка → по истории (открывается из разных мест);
   *  - верхние вкладки админки → «О приложении».
   */
  const goBack = () => {
    const errMatch = pathname.match(/^\/admin\/users\/([^/]+)\/errors$/);
    if (errMatch) return navigate(`/admin/users/${errMatch[1]}`);
    if (/^\/admin\/(users|u)\/[^/]+$/.test(pathname)) return navigate('/admin/users');
    if (/^\/admin\/catalog\/[^/]+$/.test(pathname)) return navigate('/admin/catalog');
    if (/^\/admin\/trace\/[^/]+$/.test(pathname)) {
      return window.history.length > 1 ? navigate(-1) : navigate('/admin');
    }
    return navigate('/about');
  };

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
