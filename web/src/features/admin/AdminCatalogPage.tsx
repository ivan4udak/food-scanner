import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { adminCatalog } from '@/api/admin';
import { dt } from '@/features/admin/fmt';

/** Список записей каталога. */
export function AdminCatalogPage() {
  const navigate = useNavigate();
  const q = useQuery({ queryKey: ['admin-catalog'], queryFn: () => adminCatalog(200) });

  return (
    <div className="card">
      <h2>Каталог</h2>
      {q.isLoading && <p className="muted center">Загрузка…</p>}
      {q.data && (
        <div className="table-scroll">
        <table className="admin-table">
          <thead><tr><th>ШК</th><th>Автор</th><th>Фото</th><th title="Качество 0–100">Кач.</th><th>Создано</th></tr></thead>
          <tbody>
            {q.data.map((e) => (
              <tr key={e.catalogEntryId} className="clickable" onClick={() => navigate(`/admin/catalog/${encodeURIComponent(e.barcode)}`)}>
                <td className="who">{e.barcode}</td>
                <td className="sub">{e.author ?? '—'}</td>
                <td className="num">{e.photoCount}</td>
                <td className="num strong">{e.qualityScore}</td>
                <td className="sub">{dt(e.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        </div>
      )}
    </div>
  );
}
