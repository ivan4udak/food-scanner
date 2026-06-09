/**
 * Минимальный декодер JWT-полезной нагрузки (без проверки подписи — только чтение
 * claim'ов на клиенте, например роли). Подпись валидирует backend.
 */
export function decodeJwtPayload(token: string | null): Record<string, unknown> | null {
  if (!token) return null;
  const parts = token.split('.');
  if (parts.length < 2) return null;
  try {
    let b64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    while (b64.length % 4) b64 += '=';
    const json = decodeURIComponent(
      atob(b64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join(''),
    );
    return JSON.parse(json) as Record<string, unknown>;
  } catch {
    return null;
  }
}

/** Роль из access-токена (claim `role`); по умолчанию USER. */
export function roleFromToken(token: string | null): string {
  const payload = decodeJwtPayload(token);
  const role = payload?.role;
  return typeof role === 'string' ? role.toUpperCase() : 'USER';
}

export function isAdminToken(token: string | null): boolean {
  const r = roleFromToken(token);
  return r === 'ADMIN' || r === 'SUPER_ADMIN';
}

export function isSuperAdminToken(token: string | null): boolean {
  return roleFromToken(token) === 'SUPER_ADMIN';
}
