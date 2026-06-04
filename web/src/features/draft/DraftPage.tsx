import { useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { PHOTO_TYPES, REQUIRED_PHOTO_TYPES, type PhotoType } from '@/api/types';
import { addPhoto, complete } from '@/api/catalog';
import { compressImage, readCapturedAt } from '@/lib/imageCompression';
import { ApiError } from '@/api/client';
import { useAppStore } from '@/store/appStore';
import { Page, TopBar } from '@/components/Layout';
import { PhotoSlot, type SlotState } from '@/features/draft/PhotoSlot';

const ORDER: PhotoType[] = ['BARCODE', 'FRONT', 'INGREDIENTS', 'NUTRITION', 'BACK', 'EXTRA'];

type SlotMap = Partial<Record<PhotoType, SlotState>>;

export function DraftPage() {
  const { draftId = '' } = useParams();
  const [params] = useSearchParams();
  const barcode = params.get('b') ?? '';
  const navigate = useNavigate();

  const photoSource = useAppStore((s) => s.photoSource);
  const setPhotoSource = useAppStore((s) => s.setPhotoSource);

  const [slots, setSlots] = useState<SlotMap>({});
  const [serverComplete, setServerComplete] = useState(false);
  const [uploaded, setUploaded] = useState(0);
  const [completing, setCompleting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const setSlot = (type: PhotoType, patch: Partial<SlotState>) =>
    setSlots((prev) => ({ ...prev, [type]: { status: 'empty', progress: 0, ...prev[type], ...patch } }));

  async function handlePick(type: PhotoType, file: File) {
    setError(null);
    try {
      const capturedAt = await readCapturedAt(file);
      setSlot(type, { status: 'compressing', progress: 0 });
      const compressed = await compressImage(file);
      const previewUrl = URL.createObjectURL(compressed);
      setSlot(type, { status: 'uploading', progress: 0, previewUrl });

      const res = await addPhoto({
        draftId,
        file: compressed,
        photoType: type,
        filename: `${type.toLowerCase()}.jpg`,
        capturedAt,
        onProgress: (f) => setSlot(type, { status: 'uploading', progress: f }),
      });

      setSlot(type, { status: 'done', progress: 1, previewUrl });
      setUploaded(res.uploadedCount);
      setServerComplete(res.complete);
    } catch (e) {
      setSlot(type, { status: 'error', progress: 0 });
      setError(e instanceof ApiError ? e.message : 'Не удалось загрузить фото');
    }
  }

  async function handleComplete() {
    setCompleting(true);
    setError(null);
    try {
      const res = await complete(draftId);
      navigate('/completed', { replace: true, state: { count: res.contributorCompletedCount } });
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Не удалось завершить');
    } finally {
      setCompleting(false);
    }
  }

  const useCapture = photoSource !== 'file';

  return (
    <Page>
      <TopBar title="Новый продукт" back />
      {barcode && <p className="muted">Штрихкод: {barcode}</p>}

      <div className="card">
        <div className="row">
          <span>Источник фото</span>
          <div className="actions">
            <button className={`btn ghost`} style={{ width: 'auto', opacity: useCapture ? 1 : 0.5 }} onClick={() => setPhotoSource('camera')}>
              Камера
            </button>
            <button className={`btn ghost`} style={{ width: 'auto', opacity: !useCapture ? 1 : 0.5 }} onClick={() => setPhotoSource('file')}>
              Галерея
            </button>
          </div>
        </div>
        <p className="muted" style={{ margin: 0 }}>
          Загружено обязательных: {uploaded}/{REQUIRED_PHOTO_TYPES.length}
        </p>
      </div>

      <div className="photo-grid">
        {ORDER.map((type) => (
          <PhotoSlot
            key={type}
            type={type}
            required={REQUIRED_PHOTO_TYPES.includes(type)}
            state={slots[type] ?? { status: 'empty', progress: 0 }}
            capture={useCapture}
            onPick={handlePick}
          />
        ))}
      </div>

      {error && <div className="error center">{error}</div>}

      <button className="btn" disabled={!serverComplete || completing} onClick={handleComplete}>
        {completing ? '…' : 'Завершить каталог'}
      </button>
      <p className="muted center" style={{ fontSize: '0.8rem' }}>
        Обязательны: {PHOTO_TYPES.filter((t) => REQUIRED_PHOTO_TYPES.includes(t)).join(', ')}
      </p>
    </Page>
  );
}
