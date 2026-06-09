import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { adminCatalogDetail } from '@/api/admin';
import { AuthedImage } from '@/components/AuthedImage';
import { LogTable } from '@/features/admin/LogTable';
import { dt } from '@/features/admin/fmt';

/** Деталь записи каталога: фото + связанные клиентские логи. */
export function AdminCatalogDetailPage() {
  const { barcode = '' } = useParams();
  const q = useQuery({ queryKey: ['admin-catalog', barcode], queryFn: () => adminCatalogDetail(barcode) });

  if (q.isLoading) return <p className="muted center">Загрузка…</p>;
  if (q.isError || !q.data) return <p className="error center">Запись не найдена.</p>;
  const e = q.data;

  return (
    <div className="stack">
      <div className="card">
        <h2>ШК {e.barcode}</h2>
        <div className="row"><span>Автор</span><span className="value">{e.author ?? '—'}</span></div>
        <div className="row"><span>Создано</span><span className="value">{dt(e.createdAt)}</span></div>
      </div>

      <div className="card">
        <h2>Фото ({e.photos.length})</h2>
        <div className="photo-grid">
          {e.photos.map((p) => (
            <div className="slot done" key={p.id}>
              <AuthedImage storageKey={p.storageKey} size="thumb" alt={p.type} />
              <span className="badge">{p.type}</span>
            </div>
          ))}
        </div>
      </div>

      <div className="card">
        <h2>Связанные логи</h2>
        <LogTable logs={e.relatedLogs} />
      </div>
    </div>
  );
}
