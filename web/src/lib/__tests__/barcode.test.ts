import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { nativeBarcodeAvailable, chooseEngine, createScanGate } from '@/lib/barcode';

describe('выбор движка сканера', () => {
  const original = (window as { BarcodeDetector?: unknown }).BarcodeDetector;
  afterEach(() => {
    (window as { BarcodeDetector?: unknown }).BarcodeDetector = original;
  });

  it('нет BarcodeDetector → zxing', () => {
    delete (window as { BarcodeDetector?: unknown }).BarcodeDetector;
    expect(nativeBarcodeAvailable()).toBe(false);
    expect(chooseEngine()).toBe('zxing');
  });

  it('есть BarcodeDetector → native', () => {
    (window as { BarcodeDetector?: unknown }).BarcodeDetector = function () {};
    expect(nativeBarcodeAvailable()).toBe(true);
    expect(chooseEngine()).toBe('native');
  });
});

describe('createScanGate (антидребезг)', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('блокирует повторы чаще, чем раз в ms', () => {
    const gate = createScanGate(4000);
    expect(gate.accept('4607038310042')).toBe(true);
    expect(gate.accept('4607038310042')).toBe(false);
    vi.advanceTimersByTime(4001);
    expect(gate.accept('4607038310042')).toBe(true);
  });

  it('reset снимает кулдаун', () => {
    const gate = createScanGate(4000);
    expect(gate.accept('x')).toBe(true);
    gate.reset();
    expect(gate.accept('x')).toBe(true);
  });
});
