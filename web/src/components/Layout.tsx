import type { ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';

export function Page({ children }: { children: ReactNode }) {
  return <main className="page">{children}</main>;
}

interface TopBarProps {
  title: string;
  back?: boolean;
  right?: ReactNode;
}

export function TopBar({ title, back, right }: TopBarProps) {
  const navigate = useNavigate();
  return (
    <div className="topbar">
      <div className="actions">
        {back && (
          <button className="iconbtn" aria-label="Назад" onClick={() => navigate(-1)}>
            ‹
          </button>
        )}
        <h1 style={{ fontSize: '1.25rem', margin: 0 }}>{title}</h1>
      </div>
      {right && <div className="actions">{right}</div>}
    </div>
  );
}
