import { useRef } from 'react';
import type { PhotoType } from '@/api/types';

export type SlotStatus = 'empty' | 'queued' | 'compressing' | 'uploading' | 'done' | 'error';

export interface SlotState {
  status: SlotStatus;
  progress: number;
  previewUrl?: string;
}

export const PHOTO_LABELS: Record<PhotoType, string> = {
  BARCODE: 'Штрихкод',
  FRONT: 'Лицевая',
  INGREDIENTS: 'Состав',
  NUTRITION: 'КБЖУ',
  BACK: 'Оборот',
  EXTRA: 'Дополнительно',
};

interface Props {
  type: PhotoType;
  required: boolean;
  state: SlotState;
  capture: boolean;
  onPick: (type: PhotoType, file: File) => void;
  /** Повторная отправка УЖЕ выбранного файла (без открытия камеры/галереи). */
  onRetry: (type: PhotoType) => void;
}

export function PhotoSlot({ type, required, state, capture, onPick, onRetry }: Props) {
  const inputRef = useRef<HTMLInputElement>(null);

  const handleClick = () => {
    // При ошибке — повторяем тот же файл, НЕ открывая выбор фото.
    if (state.status === 'error') {
      onRetry(type);
      return;
    }
    // Пока идёт сжатие/очередь/загрузка — игнорируем тапы.
    if (state.status === 'queued' || state.status === 'compressing' || state.status === 'uploading') {
      return;
    }
    // empty / done — открыть камеру или галерею.
    inputRef.current?.click();
  };

  return (
    <div
      className={`slot ${required ? 'required' : ''} ${state.status === 'done' ? 'done' : ''} ${
        state.status === 'error' ? 'error' : ''
      }`}
      onClick={handleClick}
    >
      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        {...(capture ? { capture: 'environment' as const } : {})}
        hidden
        onChange={(e) => {
          const f = e.target.files?.[0];
          if (f) onPick(type, f);
          e.target.value = '';
        }}
      />

      {state.previewUrl && <img src={state.previewUrl} alt={PHOTO_LABELS[type]} />}

      {state.status === 'queued' && <div className="progress">в очереди…</div>}
      {state.status === 'compressing' && <div className="progress">сжатие…</div>}
      {state.status === 'uploading' && <div className="progress">{Math.round(state.progress * 100)}%</div>}
      {state.status === 'error' && <div className="progress">ошибка ⟳ повторить</div>}

      <span className="label">
        {PHOTO_LABELS[type]}
        {required ? ' *' : ''}
      </span>
      {state.status === 'done' && <span className="badge">✓</span>}
    </div>
  );
}
