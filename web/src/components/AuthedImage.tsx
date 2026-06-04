import { useEffect, useState } from 'react';
import { fetchPhotoObjectUrl } from '@/api/catalog';
import { Spinner } from '@/components/Spinner';

interface Props {
  storageKey: string;
  size?: 'thumb' | 'full';
  alt?: string;
  className?: string;
}

/**
 * <img> для фото каталога: путь требует Bearer-заголовка, поэтому грузим байты
 * авторизованным запросом и показываем через object URL (повторно — из SW-кэша).
 */
export function AuthedImage({ storageKey, size = 'full', alt = '', className }: Props) {
  const [url, setUrl] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let active = true;
    let created: string | null = null;
    setUrl(null);
    setFailed(false);

    fetchPhotoObjectUrl(storageKey, size)
      .then((u) => {
        if (active) {
          created = u;
          setUrl(u);
        } else {
          URL.revokeObjectURL(u);
        }
      })
      .catch(() => active && setFailed(true));

    return () => {
      active = false;
      if (created) URL.revokeObjectURL(created);
    };
  }, [storageKey, size]);

  if (failed) return <span className="label muted">нет фото</span>;
  if (!url) return <Spinner />;
  return <img src={url} alt={alt} className={className} />;
}
