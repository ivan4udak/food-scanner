import { useEffect, useRef, useState } from 'react';
import { startScanner, type RunningScanner, type ScanEngine } from '@/lib/barcode';

interface Props {
  onDetected: (value: string) => void;
  paused?: boolean;
}

/** Живое сканирование с камеры (BarcodeDetector → fallback ZXing). */
export function BarcodeScanner({ onDetected, paused = false }: Props) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const onDetectedRef = useRef(onDetected);
  onDetectedRef.current = onDetected;

  const [engine, setEngine] = useState<ScanEngine | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (paused) return undefined;
    const video = videoRef.current;
    if (!video) return undefined;

    let scanner: RunningScanner | null = null;
    let cancelled = false;

    startScanner({
      video,
      cooldownMs: 4000,
      onResult: (r) => onDetectedRef.current(r.value),
    })
      .then((s) => {
        if (cancelled) {
          s.stop();
          return;
        }
        scanner = s;
        setEngine(s.engine);
        setError(null);
      })
      .catch(() => setError('Нет доступа к камере. Введите штрихкод вручную.'));

    return () => {
      cancelled = true;
      scanner?.stop();
    };
  }, [paused]);

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
