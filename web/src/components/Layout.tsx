import type { ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { aboutHref } from '@/lib/nav';

export function Page({ children }: { children: ReactNode }) {
  return <main className="page">{children}</main>;
}

interface TopBarProps {
  title: string;
  back?: boolean;
  right?: ReactNode;
  /** Кнопка-шестерёнка справа → переход на экран «О приложении» (диагностика). */
  settings?: boolean;
  /** Кастомный обработчик «Назад» (переопределяет историю). */
  onBack?: () => void;
}

export function TopBar({ title, back, right, settings, onBack }: TopBarProps) {
  const navigate = useNavigate();
  // Назад — на предыдущую страницу; если истории нет (прямой заход) — на главную.
  const goBack = onBack ?? (() => (window.history.length > 1 ? navigate(-1) : navigate('/')));

  // ⚙ → «О приложении» с returnTo (возврат сделает replace — без цикла /about ↔ страница).
  const openSettings = () =>
    navigate(aboutHref(window.location.pathname || '/', window.location.search || ''));

  return (
    <div className="topbar">
      <div className="actions">
        {back && (
          <button className="iconbtn" aria-label="Назад" onClick={goBack}>
            ‹
          </button>
        )}
        <h1 style={{ fontSize: '1.25rem', margin: 0 }}>{title}</h1>
      </div>
      {(right || settings) && (
        <div className="actions">
          {right}
          {settings && (
            <button className="iconbtn" aria-label="Настройки" onClick={openSettings}>
              ⚙
            </button>
          )}
        </div>
      )}
    </div>
  );
}
