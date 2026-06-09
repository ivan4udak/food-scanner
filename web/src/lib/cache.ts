/** Очистка кэшей PWA (Cache Storage / Workbox) и оценка занятого места. */

/** Удаляет все кэши Cache Storage. → число удалённых кэшей. */
export async function clearAppCaches(): Promise<number> {
  if (typeof caches === 'undefined') return 0;
  const keys = await caches.keys();
  const results = await Promise.all(keys.map((k) => caches.delete(k)));
  return results.filter(Boolean).length;
}

/** Просит активные service worker'ы обновиться (подтянуть свежие ассеты). */
export async function refreshServiceWorkers(): Promise<void> {
  try {
    const regs = await navigator.serviceWorker?.getRegistrations?.();
    await Promise.all((regs ?? []).map((r) => r.update()));
  } catch {
    /* нет SW / не поддерживается */
  }
}

/** Занятое место origin (Cache + IDB + localStorage), байты. null если недоступно. */
export async function estimateUsage(): Promise<number | null> {
  try {
    const e = await navigator.storage?.estimate?.();
    return e?.usage ?? 0;
  } catch {
    return null;
  }
}
