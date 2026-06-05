import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { PHOTO_TYPES, REQUIRED_PHOTO_TYPES, type PhotoType } from '@/api/types';
import { addPhoto, complete, fetchPhotoObjectUrl, getDraft } from '@/api/catalog';
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

  // Последовательная очередь загрузок: сервер (2 ядра) ресайзит по одному фото за раз,
  // иначе параллельные загрузки насыщают CPU → пинг проседает → связь «отваливается».
  const uploadQueue = useRef<Promise<unknown>>(Promise.resolve());
  // Выбранные файлы по типу — чтобы «Повторить» досылал тот же файл без повторного выбора.
  const pickedFiles = useRef<Partial<Record<PhotoType, File>>>({});

  function enqueueUpload(type: PhotoType, file: File) {
    setSlot(type, { status: 'queued', progress: 0 });
    uploadQueue.current = uploadQueue.current.then(() => doUpload(type, file));
  }

  function handlePick(type: PhotoType, file: File) {
    pickedFiles.current[type] = file; // файл уже в памяти приложения
    enqueueUpload(type, file);
  }

  // Повтор после ошибки: переотправляем тот же файл, камеру/галерею НЕ открываем.
  function handleRetry(type: PhotoType) {
    const file = pickedFiles.current[type];
    if (file) enqueueUpload(type, file);
  }

  // Восстановление при входе в черновик: подтягиваем уже загруженные фото с сервера.
  useEffect(() => {
    if (!draftId) return undefined;
    let active = true;
    getDraft(draftId)
      .then((d) => {
        if (!active || !d) return;
        setUploaded(d.uploadedCount);
        setServerComplete(d.complete);
        for (const p of d.photos) {
          const type = p.type as PhotoType;
          setSlot(type, { status: 'done', progress: 1 });
          fetchPhotoObjectUrl(p.storageKey, 'thumb')
            .then((url) => (active ? setSlot(type, { previewUrl: url }) : URL.revokeObjectURL(url)))
            .catch(() => undefined);
        }
      })
      .catch(() => undefined);
    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [draftId]);

  async function doUpload(type: PhotoType, file: File) {
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
            onRetry={handleRetry}
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
