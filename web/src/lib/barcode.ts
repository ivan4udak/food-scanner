import type { IScannerControls } from '@zxing/browser';

/**
 * Сканер штрихкодов: нативный BarcodeDetector API (Chrome/Android, частично iOS),
 * с fallback на ZXing (@zxing/browser) — он загружается динамически только если
 * нативного API нет, чтобы не тянуть либу в основной бандл/тесты.
 *
 * Поддерживаемые форматы повторяют SwiftUI-клиент: EAN-13/8, UPC-E, Code128/39/93,
 * ITF, QR, DataMatrix, PDF417, Aztec.
 */
export const NATIVE_FORMATS = [
  'ean_13', 'ean_8', 'upc_e', 'code_128', 'code_39', 'code_93',
  'itf', 'qr_code', 'data_matrix', 'pdf417', 'aztec',
];

export interface BarcodeResult {
  value: string;
  format: string;
}

export type ScanEngine = 'native' | 'zxing';

/** Доступен ли нативный BarcodeDetector. */
export function nativeBarcodeAvailable(): boolean {
  return typeof window !== 'undefined' && typeof window.BarcodeDetector === 'function';
}

export function chooseEngine(): ScanEngine {
  return nativeBarcodeAvailable() ? 'native' : 'zxing';
}

/**
 * Антидребезг сканов (как жёсткий кулдаун 4с в iOS, v1.1.9): не пропускает
 * повторные срабатывания чаще, чем раз в `ms`, и подряд одинаковые значения.
 */
export function createScanGate(ms: number) {
  let last = 0;
  let lastValue: string | null = null;
  return {
    accept(value: string): boolean {
      const now = Date.now();
      if (value === lastValue && now - last < ms) return false;
      if (now - last < ms) return false;
      last = now;
      lastValue = value;
      return true;
    },
    reset() {
      last = 0;
      lastValue = null;
    },
  };
}

export interface ScannerOptions {
  video: HTMLVideoElement;
  onResult: (r: BarcodeResult) => void;
  onError?: (e: unknown) => void;
  /** Диагностические сообщения (поток/треки/размеры/play) для клиентского лога. */
  onInfo?: (message: string, details?: unknown) => void;
  cooldownMs?: number;
}

/**
 * iOS требует для inline-воспроизведения MediaStream: muted + playsinline,
 * выставленные как СВОЙСТВА элемента (React не всегда ставит .muted), иначе
 * автозапуск блокируется и видео остаётся чёрным.
 */
function prepareVideoElement(video: HTMLVideoElement): void {
  video.muted = true;
  video.defaultMuted = true;
  video.setAttribute('muted', '');
  video.setAttribute('playsinline', 'true');
  video.setAttribute('autoplay', 'true');
  video.playsInline = true;
}

export interface RunningScanner {
  stop: () => void;
  engine: ScanEngine;
}

/**
 * Видео-ограничения: задняя камера в высоком разрешении — мелкие/дальние ШК
 * читаются заметно лучше дефолтных ~640×480.
 */
function videoConstraints(): MediaTrackConstraints {
  return {
    facingMode: { ideal: 'environment' },
    width: { ideal: 1920 },
    height: { ideal: 1080 },
  };
}

/** Непрерывный автофокус, если поддерживается — не нужно подводить ШК вплотную. */
async function enableContinuousFocus(video: HTMLVideoElement): Promise<void> {
  try {
    const stream = video.srcObject as MediaStream | null;
    const track = stream?.getVideoTracks?.()[0];
    if (track) {
      await track.applyConstraints({ advanced: [{ focusMode: 'continuous' }] } as unknown as MediaTrackConstraints);
    }
  } catch {
    /* устройство не поддерживает — не критично */
  }
}

/** Запускает непрерывное сканирование с задней камеры. */
export async function startScanner(opts: ScannerOptions): Promise<RunningScanner> {
  const gate = createScanGate(opts.cooldownMs ?? 1500);
  const engine = chooseEngine();

  if (engine === 'native') {
    return startNative(opts, gate);
  }
  return startZxing(opts, gate);
}

async function startNative(opts: ScannerOptions, gate: ReturnType<typeof createScanGate>): Promise<RunningScanner> {
  const stream = await navigator.mediaDevices.getUserMedia({
    video: videoConstraints(),
    audio: false,
  });
  opts.onInfo?.('camera stream acquired', {
    tracks: stream.getVideoTracks().map((t) => ({ label: t.label, state: t.readyState })),
  });
  prepareVideoElement(opts.video);
  opts.video.srcObject = stream;
  try {
    await opts.video.play();
  } catch (e) {
    opts.onInfo?.('video.play() rejected', { name: (e as Error).name, message: (e as Error).message });
    throw e;
  }
  await enableContinuousFocus(opts.video);
  opts.onInfo?.('video playing', { w: opts.video.videoWidth, h: opts.video.videoHeight });

  const detector = new window.BarcodeDetector!({ formats: NATIVE_FORMATS });
  let stopped = false;

  const tick = async () => {
    if (stopped) return;
    try {
      const codes = await detector.detect(opts.video);
      if (codes.length && gate.accept(codes[0].rawValue)) {
        opts.onResult({ value: codes[0].rawValue, format: codes[0].format });
      }
    } catch (e) {
      opts.onError?.(e);
    }
    if (!stopped) setTimeout(tick, 120); // ~8 проверок/сек
  };
  void tick();

  return {
    engine: 'native',
    stop: () => {
      stopped = true;
      stream.getTracks().forEach((t) => t.stop());
      opts.video.srcObject = null;
    },
  };
}

async function startZxing(opts: ScannerOptions, gate: ReturnType<typeof createScanGate>): Promise<RunningScanner> {
  const { BrowserMultiFormatReader } = await import('@zxing/browser');
  const { DecodeHintType, BarcodeFormat } = await import('@zxing/library');

  // Ограничиваем форматы (товарные ШК) + TRY_HARDER — быстрее и чувствительнее,
  // чем перебор всех форматов на каждом кадре.
  const hints = new Map<number, unknown>();
  hints.set(DecodeHintType.POSSIBLE_FORMATS, [
    BarcodeFormat.EAN_13, BarcodeFormat.EAN_8, BarcodeFormat.UPC_A, BarcodeFormat.UPC_E,
    BarcodeFormat.CODE_128, BarcodeFormat.CODE_39, BarcodeFormat.ITF, BarcodeFormat.QR_CODE,
  ]);
  hints.set(DecodeHintType.TRY_HARDER, true);

  // delayBetweenScanAttempts 100мс (дефолт 500 = всего 2 попытки/сек → «долго думает»).
  const reader = new BrowserMultiFormatReader(hints as never, {
    delayBetweenScanAttempts: 100,
    delayBetweenScanSuccess: 1000,
  });
  let controls: IScannerControls | null = null;

  // iOS: muted+playsinline ДО привязки потока (ZXing сам зовёт play()).
  prepareVideoElement(opts.video);

  controls = await reader.decodeFromConstraints(
    { video: videoConstraints(), audio: false },
    opts.video,
    (result, err) => {
      if (result) {
        const value = result.getText();
        if (gate.accept(value)) {
          opts.onResult({ value, format: String(result.getBarcodeFormat()) });
        }
      } else if (err && err.name !== 'NotFoundException') {
        opts.onError?.(err);
      }
    },
  );

  await enableContinuousFocus(opts.video);
  opts.onInfo?.('zxing started', { w: opts.video.videoWidth, h: opts.video.videoHeight });

  return {
    engine: 'zxing',
    stop: () => controls?.stop(),
  };
}
