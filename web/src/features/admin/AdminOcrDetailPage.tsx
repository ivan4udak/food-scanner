import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminOcrDetail, adminReprocessOcr } from '@/api/admin';
import { AuthedImage } from '@/components/AuthedImage';
import { PhotoLightbox } from '@/components/PhotoLightbox';
import { dt } from '@/features/admin/fmt';
import { OCR_STATUS_SHORT, ocrStatusKind } from '@/features/admin/ocr';

const KIND_COLOR: Record<string, string> = { gray: '#9aa0a6', green: '#34a853', yellow: '#f9ab00', red: '#ea4335' };
const isActive = (s: number) => s === 0 || s === 1;
const canReprocess = (s: number) => s === 2 || s === 3 || s === 5;

/** Полная карточка OCR-задачи: статус, полный rawText (копирование), связи, фото, reprocess. */
export function AdminOcrDetailPage() {
  const { jobId = '' } = useParams();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [zoom, setZoom] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const q = useQuery({
    queryKey: ['admin-ocr-detail', jobId],
    queryFn: () => adminOcrDetail(jobId),
    refetchInterval: (query) => (isActive(query.state.data?.statusCode ?? -1) ? 5000 : false),
  });
  const reprocess = useMutation({
    mutationFn: () => adminReprocessOcr(jobId),
    onSuccess: () => qc.invalidateQueries(),
  });

  if (q.isLoading) return <p className="muted center">Загрузка…</p>;
  if (q.isError || !q.data) return <p className="error center">Задача не найдена.</p>;
  const j = q.data;

  const copyRaw = async () => {
    if (!j.rawText) return;
    try {
      await navigator.clipboard.writeText(j.rawText);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch { /* ignore */ }
  };

  return (
    <div className="stack">
      <div className="card">
        <h2>
          <span style={{ display: 'inline-block', width: 10, height: 10, borderRadius: '50%',
            background: KIND_COLOR[ocrStatusKind(j.statusCode)], marginRight: 8 }} />
          {OCR_STATUS_SHORT[j.statusCode] ?? j.status} · {j.photoType}
        </h2>
        <div className="row"><span>ШК</span><span className="value">
          {j.barcode
            ? <button className="chip" onClick={() => navigate(`/admin/catalog/${encodeURIComponent(j.barcode!)}`)}>{j.barcode}</button>
            : '—'}
        </span></div>
        <div className="row"><span>Автор</span><span className="value">
          {j.contributorId
            ? <button className="chip" onClick={() => navigate(`/admin/users/${j.contributorId}`)}>{j.author ?? j.contributorId}</button>
            : '—'}
        </span></div>
        <div className="row"><span>Активна / orphaned</span><span className="value">{String(j.active)} / {String(j.orphaned)}</span></div>
        <div className="row"><span>Попытки</span><span className="value">{j.attempts}</span></div>
        <div className="row"><span>Confidence</span><span className="value">{j.confidence ?? '—'}</span></div>
        <div className="row"><span>Создано / обновлено</span><span className="value">{dt(j.createdAt)} · {dt(j.updatedAt)}</span></div>
        {(j.errorCode || j.errorMessage) && (
          <div className="row"><span>Ошибка</span><span className="value">{j.errorCode ?? ''} {j.errorMessage ?? ''}</span></div>
        )}
        {canReprocess(j.statusCode) && (
          <button className="btn secondary" disabled={reprocess.isPending} onClick={() => reprocess.mutate()}>
            ↻ Переотправить в OCR
          </button>
        )}
      </div>

      <div className="card">
        <h2>Структурные данные</h2>
        {(j.parsedName || j.parsedBrand || j.parsedManufacturer || j.parsedIngredients || j.parsedNutrition)
          ? (<>
              {j.parsedName && <div className="row"><span>Название</span><span className="value">{j.parsedName}</span></div>}
              {j.parsedBrand && <div className="row"><span>Бренд</span><span className="value">{j.parsedBrand}</span></div>}
              {j.parsedManufacturer && <div className="row"><span>Производитель</span><span className="value">{j.parsedManufacturer}</span></div>}
              {j.parsedIngredients && <div className="row"><span>Состав</span><span className="value">{j.parsedIngredients}</span></div>}
              {j.parsedNutrition && <div className="row"><span>КБЖУ</span><span className="value">{j.parsedNutrition}</span></div>}
            </>)
          : <p className="muted">Пока не извлечено (структурное извлечение — следующий срез).</p>}
      </div>

      <div className="card">
        <h2>Распознанный текст</h2>
        {j.rawText
          ? (<>
              <button className="btn ghost" style={{ width: 'auto' }} onClick={copyRaw}>
                {copied ? 'Скопировано ✓' : 'Скопировать rawText'}
              </button>
              <pre className="logview" style={{ whiteSpace: 'pre-wrap' }}>{j.rawText}</pre>
            </>)
          : <p className="muted">Нет текста (заглушка движка либо фото нечитаемо).</p>}
      </div>

      <div className="card">
        <h2>Публикация</h2>
        <div className="row"><span>published_at</span><span className="value">{dt(j.publishedAt)}</span></div>
        <div className="row"><span>publish attempts</span><span className="value">{j.publishAttempts}</span></div>
        {j.lastPublishError && <div className="row"><span>last publish error</span><span className="value">{j.lastPublishError}</span></div>}
      </div>

      <div className="card">
        <h2>Фото</h2>
        <div className="photo-grid">
          <div className="slot done zoomable" onClick={() => setZoom(j.storageKey)}>
            <AuthedImage storageKey={j.storageKey} size="thumb" alt={j.photoType} />
            <span className="badge">{j.photoType}</span>
          </div>
        </div>
        <div className="sub">{j.storageKey}</div>
      </div>

      {zoom && <PhotoLightbox storageKey={zoom} onClose={() => setZoom(null)} />}
    </div>
  );
}
