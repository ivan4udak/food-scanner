import type { ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';

export function Page({ children }: { children: ReactNode }) {
  return <main className="page">{children}</main>;
}

interface TopBarProps {
  title: string;
  back?: boolean;
  right?: ReactNode;
  /** Кнопка-шестерёнка справа → переход на экран «О приложении» (диагностика). */
  settings?: boolean;
}

export function TopBar({ title, back, right, settings }: TopBarProps) {
  const navigate = useNavigate();
  // Назад — на предыдущую страницу; если истории нет (прямой заход) — на главную.
  const goBack = () => (window.history.length > 1 ? navigate(-1) : navigate('/'));
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
            <button className="iconbtn" aria-label="Настройки" onClick={() => navigate('/about')}>
              ⚙
            </button>
          )}
        </div>
      )}
    </div>
  );
}
