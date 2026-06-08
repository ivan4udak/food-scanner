import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getMyScans } from '@/api/me';
import { Page, TopBar } from '@/components/Layout';

const STATUS_LABEL: Record<string, string> = {
  COMPLETED: 'В каталоге',
  DRAFT_OPEN: 'Черновик',
};

function dt(iso: string | null): string {
  if (!iso) return '';
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? '' : d.toLocaleDateString('ru-RU', { dateStyle: 'medium' });
}

/** «Мои сканы» — список штрихкодов пользователя. */
export function MyScansPage() {
  const navigate = useNavigate();
  const q = useQuery({ queryKey: ['my-scans'], queryFn: getMyScans });

  return (
    <Page>
      <TopBar title="Мои сканы" back settings />

      {q.isLoading && <p className="muted center">Загрузка…</p>}
      {q.isError && <p className="error center">Не удалось загрузить.</p>}
      {q.data && q.data.length === 0 && <div className="list-empty">Пока нет сканов.</div>}

      <div className="stack">
        {q.data?.map((s) => (
          <div
            key={s.barcode}
            className="card clickable row"
            onClick={() => navigate(`/my-scans/${encodeURIComponent(s.barcode)}`)}
          >
            <div>
              <div className="who" style={{ fontWeight: 600 }}>{s.barcode}</div>
              <div className="sub">{STATUS_LABEL[s.scanStatus] ?? s.scanStatus} · {dt(s.firstScannedAt)}</div>
            </div>
            <span className="value">{s.photoCount} фото ›</span>
          </div>
        ))}
      </div>
    </Page>
  );
}
