import { describe, it, expect } from 'vitest';
import { adminBackTarget, aboutReturnTarget, aboutHref } from '@/lib/nav';

describe('adminBackTarget — иерархический назад', () => {
  it('leaf → родительский список', () => {
    expect(adminBackTarget('/admin/users/123')).toBe('/admin/users');
    expect(adminBackTarget('/admin/u/ivan')).toBe('/admin/users');
    expect(adminBackTarget('/admin/catalog/4680')).toBe('/admin/catalog');
    expect(adminBackTarget('/admin/ocr/job-1')).toBe('/admin/ocr');
    expect(adminBackTarget('/admin/users/123/errors')).toBe('/admin/users/123');
  });
  it('trace → по истории (null)', () => {
    expect(adminBackTarget('/admin/trace/corr-1')).toBeNull();
  });
  it('верхние вкладки → /about', () => {
    expect(adminBackTarget('/admin/ocr')).toBe('/about');
    expect(adminBackTarget('/admin/users')).toBe('/about');
  });
});

describe('aboutReturnTarget — без цикла', () => {
  it('возврат на returnTo (внутренний путь)', () => {
    expect(aboutReturnTarget('?returnTo=%2Fadmin%2Focr')).toBe('/admin/ocr');
  });
  it('нет returnTo → на главную (не зацикливаемся на admin)', () => {
    expect(aboutReturnTarget('')).toBe('/');
    expect(aboutReturnTarget('?x=1')).toBe('/');
  });
  it('внешний/протокольный путь игнорируется', () => {
    expect(aboutReturnTarget('?returnTo=https://evil')).toBe('/');
    expect(aboutReturnTarget('?returnTo=//evil')).toBe('/');
  });
});

describe('aboutHref', () => {
  it('кодирует текущий путь в returnTo', () => {
    expect(aboutHref('/admin/ocr', '?status=2')).toBe('/about?returnTo=%2Fadmin%2Focr%3Fstatus%3D2');
  });
});
