import type { ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';

export function Page({ children }: { children: ReactNode }) {
  return <main className="page">{children}</main>;
}

/** Ключ запоминания страницы, с которой открыли «О приложении» (через ⚙). */
export const ABOUT_FROM_KEY = 'foodscanner.about.from';

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

  // ⚙ запоминает текущую страницу, чтобы «Назад» из «О приложении» вернул именно сюда.
  const openSettings = () => {
    try {
      sessionStorage.setItem(ABOUT_FROM_KEY, window.location.pathname || '/');
    } catch {
      /* ignore */
    }
    navigate('/about');
  };

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
