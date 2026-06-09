import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getMyScanDetail } from '@/api/me';
import { AuthedImage } from '@/components/AuthedImage';
import { Page, TopBar } from '@/components/Layout';

function dt(iso: string | null): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? '—' : d.toLocaleString('ru-RU', { dateStyle: 'medium', timeStyle: 'short' });
}

/** Детали скана: фото пользователя. OCR-блок не показываем до v1.10. */
export function MyScanDetailPage() {
  const { barcode = '' } = useParams();
  const q = useQuery({ queryKey: ['my-scan', barcode], queryFn: () => getMyScanDetail(barcode) });

  return (
    <Page>
      <TopBar title={barcode} back settings />

      {q.isLoading && <p className="muted center">Загрузка…</p>}
      {q.isError && <p className="error center">Скан не найден.</p>}

      {q.data && (
        <>
          <div className="card">
            <div className="row"><span>Штрихкод</span><span className="value">{q.data.barcode}</span></div>
            <div className="row"><span>Статус</span><span className="value">{q.data.completedAt ? 'В каталоге' : 'Черновик'}</span></div>
            <div className="row"><span>Первый скан</span><span className="value">{dt(q.data.firstScannedAt)}</span></div>
          </div>

          <div className="card">
            <h2>Фото ({q.data.photos.length})</h2>
            {q.data.photos.length === 0 ? (
              <p className="muted">Фото нет.</p>
            ) : (
              <div className="photo-grid">
                {q.data.photos.map((p) => (
                  <div className="slot done" key={p.id}>
                    <AuthedImage storageKey={p.storageKey} size="thumb" alt={p.type} />
                    <span className="badge">{p.type}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </>
      )}
    </Page>
  );
}
