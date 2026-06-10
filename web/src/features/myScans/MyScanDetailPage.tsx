import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getMyScanDetail, type MyScanOcr } from '@/api/me';
import { AuthedImage } from '@/components/AuthedImage';
import { PhotoLightbox } from '@/components/PhotoLightbox';
import { Page, TopBar } from '@/components/Layout';

function dt(iso: string | null): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? '—' : d.toLocaleString('ru-RU', { dateStyle: 'medium', timeStyle: 'short' });
}

// Клиентская подпись/цвет статуса OCR (QUEUED — ожидание, не ошибка).
const OCR_LABEL: Record<number, string> = {
  0: 'Ожидает распознавания', 1: 'Распознаётся…', 2: 'Требует проверки',
  3: 'Не читается', 4: 'Распознано', 5: 'Ошибка распознавания',
};
const OCR_COLOR: Record<number, string> = {
  0: '#9aa0a6', 1: '#9aa0a6', 2: '#f9ab00', 3: '#ea4335', 4: '#34a853', 5: '#ea4335',
};
const isActiveOcr = (s: number) => s === 0 || s === 1;

/** Детали скана: фото + OCR-статусы (polling до финала). */
export function MyScanDetailPage() {
  const { barcode = '' } = useParams();
  const [zoom, setZoom] = useState<string | null>(null);
  const q = useQuery({
    queryKey: ['my-scan', barcode],
    queryFn: () => getMyScanDetail(barcode),
    // пока есть незавершённые OCR (QUEUED/IN_PROGRESS) — опрашиваем; иначе останавливаемся
    refetchInterval: (query) =>
      (query.state.data?.ocr ?? []).some((o: MyScanOcr) => isActiveOcr(o.statusCode)) ? 5000 : false,
  });

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
                  <div className="slot done zoomable" key={p.id} onClick={() => setZoom(p.storageKey)}>
                    <AuthedImage storageKey={p.storageKey} size="thumb" alt={p.type} />
                    <span className="badge">{p.type}</span>
                  </div>
                ))}
              </div>
            )}
          </div>

          {q.data.ocr.length > 0 && (
            <div className="card">
              <h2>Распознавание</h2>
              {q.data.ocr.map((o) => (
                <div className="row" key={o.photoType}>
                  <span>{o.photoType}</span>
                  <span className="value">
                    <span style={{
                      display: 'inline-block', width: 8, height: 8, borderRadius: '50%',
                      background: OCR_COLOR[o.statusCode] ?? '#9aa0a6', marginRight: 6,
                    }} />
                    {OCR_LABEL[o.statusCode] ?? o.status}
                  </span>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {zoom && <PhotoLightbox storageKey={zoom} onClose={() => setZoom(null)} />}
    </Page>
  );
}
