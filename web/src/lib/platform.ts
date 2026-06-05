/**
 * Определение платформы и режима запуска PWA.
 */

/** Запущено ли приложение как установленная PWA (standalone). */
export function isStandalone(): boolean {
  if (typeof window === 'undefined') return false;
  const displayStandalone =
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(display-mode: standalone)').matches;
  // iOS Safari выставляет navigator.standalone в standalone-режиме.
  const iosStandalone = (window.navigator as Navigator & { standalone?: boolean }).standalone === true;
  return Boolean(displayStandalone || iosStandalone);
}

/** iPhone/iPad/iPod (включая iPadOS, который маскируется под Mac). */
export function isIOS(): boolean {
  if (typeof navigator === 'undefined') return false;
  const ua = navigator.userAgent || '';
  const iDevice = /iPad|iPhone|iPod/.test(ua);
  const iPadOS =
    navigator.platform === 'MacIntel' &&
    typeof navigator.maxTouchPoints === 'number' &&
    navigator.maxTouchPoints > 1;
  return iDevice || iPadOS;
}

/** Android. */
export function isAndroid(): boolean {
  if (typeof navigator === 'undefined') return false;
  return /Android/i.test(navigator.userAgent || '');
}
