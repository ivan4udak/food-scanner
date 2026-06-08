import { useParams } from 'react-router-dom';
import { useEntryQuery } from '@/hooks/queries';
import { AuthedImage } from '@/components/AuthedImage';
import { FullScreenSpinner } from '@/components/Spinner';
import { Page, TopBar } from '@/components/Layout';
import { PHOTO_LABELS } from '@/features/draft/PhotoSlot';
import type { PhotoType } from '@/api/types';

export function LookupPage() {
  const { barcode = '' } = useParams();
  const { data, isLoading, isError } = useEntryQuery(barcode);

  if (isLoading) return <FullScreenSpinner />;

  return (
    <Page>
      <TopBar title="Продукт" back settings />
      <p className="muted">Штрихкод: {barcode}</p>

      {isError && <div className="error">Не удалось загрузить запись.</div>}
      {!data && !isError && <div className="list-empty">Запись не найдена в каталоге.</div>}

      {data && (
        <>
          <div className="photo-grid">
            {data.photos.map((p) => (
              <div className="slot done" key={p.id}>
                <AuthedImage storageKey={p.storageKey} size="thumb" alt={p.type} />
                <span className="label">{PHOTO_LABELS[p.type as PhotoType] ?? p.type}</span>
              </div>
            ))}
          </div>
          <p className="muted center" style={{ fontSize: '0.8rem' }}>
            Добавлено: {new Date(data.createdAt).toLocaleString('ru-RU')}
          </p>
        </>
      )}
    </Page>
  );
}
