import { api, ApiError, parseOrThrow } from '@/api/client';
import { AuthResponseSchema, type AuthResponse, type Session } from '@/api/types';

/** Исход входа (как LoginOutcome в iOS-клиенте). */
export type LoginOutcome =
  | { kind: 'ok'; session: Session }
  | { kind: 'recovery'; username: string }
  | { kind: 'notFound' }
  | { kind: 'invalid'; message: string }
  | { kind: 'locked'; message: string };

function sessionFrom(a: AuthResponse, fallbackUser: string): Session | null {
  if (a.contributorId && a.accessToken && a.refreshToken) {
    return {
      contributorId: a.contributorId,
      username: a.username ?? fallbackUser,
      accessToken: a.accessToken,
      refreshToken: a.refreshToken,
    };
  }
  return null;
}

/** POST /auth/login — читает тела 401/404/423 без выброса. */
export async function login(username: string, password: string): Promise<LoginOutcome> {
  const res = await api.post('/auth/login', { username, password }, { validateStatus: () => true });
  const data = AuthResponseSchema.safeParse(res.data).data;

  switch (res.status) {
    case 200:
      if (data?.status === 'RECOVERY') return { kind: 'recovery', username: data.username ?? username };
      {
        const session = data ? sessionFrom(data, username) : null;
        if (session) return { kind: 'ok', session };
      }
      throw new ApiError('Пустой ответ сервера', 502);
    case 404:
      return { kind: 'notFound' };
    case 401:
      return { kind: 'invalid', message: data?.message ?? 'Неверный логин или пароль' };
    case 423:
      return { kind: 'locked', message: data?.message ?? 'Аккаунт временно заблокирован' };
    default:
      throw new ApiError(data?.message ?? 'Ошибка входа', res.status);
  }
}

/** POST /auth/register — 201 → сессия; 409 занят; 400 пароль вне 4..100. */
export async function register(username: string, password: string): Promise<Session> {
  const res = await api.post('/auth/register', { username, password }, { validateStatus: () => true });
  if (res.status === 201) {
    const data = parseOrThrow(AuthResponseSchema, res.data);
    const session = sessionFrom(data, username);
    if (session) return session;
  }
  if (res.status === 409) throw new ApiError('Логин уже занят', 409);
  const data = AuthResponseSchema.safeParse(res.data).data;
  throw new ApiError(data?.message ?? 'Не удалось создать аккаунт', res.status);
}

/** POST /auth/recover — новый пароль в окне восстановления (5 мин). */
export async function recover(username: string, password: string): Promise<Session> {
  const res = await api.post('/auth/recover', { username, password }, { validateStatus: () => true });
  if (res.status === 200) {
    const data = parseOrThrow(AuthResponseSchema, res.data);
    const session = sessionFrom(data, username);
    if (session) return session;
  }
  const data = AuthResponseSchema.safeParse(res.data).data;
  throw new ApiError(data?.message ?? 'Окно восстановления истекло', res.status);
}
