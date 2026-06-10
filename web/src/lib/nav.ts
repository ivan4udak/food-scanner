/**
 * Навигационные правила (чистые функции — без react-router, тестируемо).
 * v1.10.4: чиним цикл /about ↔ admin leaf и иерархический «Назад».
 */

/**
 * Куда вести «Назад» из админ-страницы по её pathname.
 * Возвращает путь, либо null = «назад по истории» (для trace, открываемого из разных мест).
 */
export function adminBackTarget(pathname: string): string | null {
  const err = pathname.match(/^\/admin\/users\/([^/]+)\/errors$/);
  if (err) return `/admin/users/${err[1]}`;
  if (/^\/admin\/(users|u)\/[^/]+$/.test(pathname)) return '/admin/users';
  if (/^\/admin\/catalog\/[^/]+$/.test(pathname)) return '/admin/catalog';
  if (/^\/admin\/ocr\/[^/]+$/.test(pathname)) return '/admin/ocr';
  if (/^\/admin\/trace\/[^/]+$/.test(pathname)) return null; // по истории
  return '/about'; // верхние вкладки админки → «О приложении»
}

/**
 * Куда вернуться из /about. Берём ?returnTo (только внутренний путь), иначе на главную.
 * Возврат делается с replace — /about не остаётся в истории (нет цикла).
 */
export function aboutReturnTarget(search: string): string {
  const rt = new URLSearchParams(search).get('returnTo');
  return rt && rt.startsWith('/') && !rt.startsWith('//') ? rt : '/';
}

/** URL экрана «О приложении» с запоминанием, откуда открыли (для возврата). */
export function aboutHref(pathname: string, search = ''): string {
  return `/about?returnTo=${encodeURIComponent(pathname + search)}`;
}
