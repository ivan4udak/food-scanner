import { useEffect, useRef, useState } from 'react';
import { startScanner, type RunningScanner, type ScanEngine } from '@/lib/barcode';
import { logger } from '@/logging/logger';

interface Props {
  onDetected: (value: string) => void;
  /** Пауза только ПриёмА результатов; камера-поток не останавливается (важно для iOS). */
  paused?: boolean;
}

/**
 * Живое сканирование с камеры (BarcodeDetector → fallback ZXing).
 *
 * Камера запускается один раз при монтировании и живёт весь жизненный цикл
 * экрана. `paused` лишь приостанавливает приём штрихкодов (например, на время
 * сетевого запроса/деградации), НЕ пересоздавая поток — иначе на iOS частые
 * stop/start оставляют чёрный кадр.
 */
export function BarcodeScanner({ onDetected, paused = false }: Props) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const onDetectedRef = useRef(onDetected);
  onDetectedRef.current = onDetected;
  const pausedRef = useRef(paused);
  pausedRef.current = paused;

  const [engine, setEngine] = useState<ScanEngine | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Камера — один раз на монтирование (без зависимости от paused).
  useEffect(() => {
    const video = videoRef.current;
    if (!video) return undefined;

    let scanner: RunningScanner | null = null;
    let cancelled = false;

    logger.info('SCAN', 'Scanner opened');
    startScanner({
      video,
      cooldownMs: 4000,
      onInfo: (m, d) => logger.debug('SCAN', m, d),
      onResult: (r) => {
        if (pausedRef.current) return; // детект приостановлен — поток не трогаем
        logger.info('SCAN', 'Barcode detected', { value: r.value });
        onDetectedRef.current(r.value);
      },
    })
      .then((s) => {
        if (cancelled) {
          s.stop();
          return;
        }
        scanner = s;
        setEngine(s.engine);
        setError(null);
        logger.info('SCAN', `Scanner running (${s.engine})`);
      })
      .catch((e: unknown) => {
        const err = e as Error;
        logger.warn('SCAN', 'Camera unavailable', { name: err?.name, message: err?.message });
        setError('Нет доступа к камере. Проверьте разрешение и введите штрихкод вручную.');
      });

    return () => {
      cancelled = true;
      scanner?.stop();
      logger.debug('SCAN', 'Scanner closed');
    };
  }, []);

  return (
    <div className="scanner">
      <video ref={videoRef} muted playsInline autoPlay />
      <div className="frame" />
      {error && (
        <div className="progress" style={{ padding: 20, textAlign: 'center' }}>
          {error}
        </div>
      )}
      {engine === 'zxing' && !error && (
        <div className="badge" style={{ left: 6, right: 'auto' }}>
          ZXing
        </div>
      )}
    </div>
  );
}
