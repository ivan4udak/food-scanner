import { NavLink, Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';
import { Page } from '@/components/Layout';
import { adminBackTarget } from '@/lib/nav';

const TABS = [
  { to: '/admin', label: 'Дашборд', end: true },
  { to: '/admin/users', label: 'Пользователи' },
  { to: '/admin/logs', label: 'Логи' },
  { to: '/admin/catalog', label: 'Каталог' },
  { to: '/admin/ocr', label: 'OCR' },
  { to: '/admin/extraction', label: 'Извлечение' },
  { to: '/admin/errors', label: 'Ошибки' },
];

/** Каркас админ-панели: гард доступа (только ADMIN) + навигация. */
export function AdminLayout() {
  const isAdmin = useAuthStore((s) => s.isAdmin);
  const navigate = useNavigate();
  const { pathname } = useLocation();

  if (!isAdmin()) return <Navigate to="/" replace />;

  /**
   * Иерархический «Назад» (правила — в lib/nav.adminBackTarget):
   *  leaf → родительский список; trace → по истории; верхние вкладки → «О приложении».
   * Возврат на верхнюю вкладку идёт на /about БЕЗ returnTo → дальше «Назад» из /about
   * ведёт на главную, а не обратно в админку (нет цикла /about ↔ admin).
   */
  const goBack = () => {
    const target = adminBackTarget(pathname);
    if (target === null) return window.history.length > 1 ? navigate(-1) : navigate('/admin');
    return navigate(target);
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
