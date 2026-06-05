import { describe, it, expect, afterEach, vi } from 'vitest';
import { isStandalone, isIOS, isAndroid } from '@/lib/platform';

function setUA(ua: string) {
  Object.defineProperty(navigator, 'userAgent', { value: ua, configurable: true });
}

afterEach(() => {
  vi.unstubAllGlobals();
  (window.navigator as Navigator & { standalone?: boolean }).standalone = false;
});

describe('isIOS / isAndroid', () => {
  it('iPhone → iOS, не Android', () => {
    setUA('Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15');
    expect(isIOS()).toBe(true);
    expect(isAndroid()).toBe(false);
  });
  it('Android → Android, не iOS', () => {
    setUA('Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36');
    expect(isAndroid()).toBe(true);
    expect(isIOS()).toBe(false);
  });
});

describe('isStandalone', () => {
  it('false в обычной вкладке', () => {
    vi.stubGlobal('matchMedia', () => ({ matches: false }));
    (window.navigator as Navigator & { standalone?: boolean }).standalone = false;
    expect(isStandalone()).toBe(false);
  });
  it('true при display-mode: standalone', () => {
    vi.stubGlobal('matchMedia', (q: string) => ({ matches: q.includes('standalone') }));
    expect(isStandalone()).toBe(true);
  });
  it('true при navigator.standalone (iOS)', () => {
    vi.stubGlobal('matchMedia', () => ({ matches: false }));
    (window.navigator as Navigator & { standalone?: boolean }).standalone = true;
    expect(isStandalone()).toBe(true);
  });
});
