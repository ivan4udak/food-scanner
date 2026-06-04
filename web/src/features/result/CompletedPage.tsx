import { useLocation, useNavigate } from 'react-router-dom';
import { Page } from '@/components/Layout';

export function CompletedPage() {
  const navigate = useNavigate();
  const state = useLocation().state as { count?: number } | null;
  const count = state?.count;

  return (
    <Page>
      <div className="grow" />
      <div className="center stack">
        <div style={{ fontSize: '4rem' }}>✓</div>
        <h1>Каталог завершён</h1>
        <p>Все обязательные фото загружены, запись создана.</p>
        {typeof count === 'number' && <p className="muted">Ваших завершённых каталогов: {count}</p>}
      </div>
      <div className="grow" />
      <button className="btn" onClick={() => navigate('/', { replace: true })}>
        Сканировать ещё
      </button>
    </Page>
  );
}
