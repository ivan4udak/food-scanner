import { useEffect } from 'react';
import { AuthedImage } from '@/components/AuthedImage';

interface Props {
  storageKey: string;
  onClose: () => void;
}

/**
 * Полноэкранный просмотр фото в полном качестве (size=full, грузится с сервера
 * авторизованным запросом). Закрытие по клику/Esc.
 */
export function PhotoLightbox({ storageKey, onClose }: Props) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  return (
    <div className="lightbox" onClick={onClose} role="dialog" aria-modal="true">
      <button className="lightbox-close" aria-label="Закрыть" onClick={onClose}>✕</button>
      <div className="lightbox-body" onClick={(e) => e.stopPropagation()}>
        <AuthedImage storageKey={storageKey} size="full" className="lightbox-img" />
      </div>
    </div>
  );
}
