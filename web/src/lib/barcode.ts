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
  cooldownMs?: number;
}

export interface RunningScanner {
  stop: () => void;
  engine: ScanEngine;
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
    video: { facingMode: 'environment' },
    audio: false,
  });
  opts.video.srcObject = stream;
  await opts.video.play();

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
    if (!stopped) setTimeout(tick, 200);
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
  const reader = new BrowserMultiFormatReader();
  let controls: IScannerControls | null = null;

  controls = await reader.decodeFromConstraints(
    { video: { facingMode: 'environment' }, audio: false },
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

  return {
    engine: 'zxing',
    stop: () => controls?.stop(),
  };
}
