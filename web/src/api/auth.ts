import { api, ApiError, parseOrThrow } from '@/api/client';
import { AuthResponseSchema, type AuthResponse, type Session } from '@/api/types';
import { logger } from '@/logging/logger';

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
  logger.info('AUTH', 'Login started', { username });
  const res = await api.post('/auth/login', { username, password }, { validateStatus: () => true });
  const data = AuthResponseSchema.safeParse(res.data).data;

  switch (res.status) {
    case 200:
      if (data?.status === 'RECOVERY') {
        logger.info('AUTH', 'Login → recovery required', { username });
        return { kind: 'recovery', username: data.username ?? username };
      }
      {
        const session = data ? sessionFrom(data, username) : null;
        if (session) {
          logger.info('AUTH', 'Login success', { username });
          return { kind: 'ok', session };
        }
      }
      logger.error('AUTH', 'Login: empty server response');
      throw new ApiError('Пустой ответ сервера', 502);
    case 404:
      logger.warn('AUTH', 'Login failed: not found', { username });
      return { kind: 'notFound' };
    case 401:
      logger.warn('AUTH', 'Login failed: invalid credentials', { username });
      return { kind: 'invalid', message: data?.message ?? 'Неверный логин или пароль' };
    case 423:
      logger.warn('AUTH', 'Login failed: locked', { username });
      return { kind: 'locked', message: data?.message ?? 'Аккаунт временно заблокирован' };
    default:
      logger.error('AUTH', `Login error HTTP ${res.status}`, { username });
      throw new ApiError(data?.message ?? 'Ошибка входа', res.status);
  }
}

/** POST /auth/register — 201 → сессия; 409 занят; 400 пароль вне 4..100. */
export async function register(username: string, password: string): Promise<Session> {
  logger.info('AUTH', 'Register started', { username });
  const res = await api.post('/auth/register', { username, password }, { validateStatus: () => true });
  if (res.status === 201) {
    const data = parseOrThrow(AuthResponseSchema, res.data);
    const session = sessionFrom(data, username);
    if (session) {
      logger.info('AUTH', 'Register success', { username });
      return session;
    }
  }
  if (res.status === 409) {
    logger.warn('AUTH', 'Register failed: username taken', { username });
    throw new ApiError('Логин уже занят', 409);
  }
  const data = AuthResponseSchema.safeParse(res.data).data;
  logger.warn('AUTH', `Register failed HTTP ${res.status}`, { username });
  throw new ApiError(data?.message ?? 'Не удалось создать аккаунт', res.status);
}

/** POST /auth/recover — новый пароль в окне восстановления (5 мин). */
export async function recover(username: string, password: string): Promise<Session> {
  logger.info('AUTH', 'Recover started', { username });
  const res = await api.post('/auth/recover', { username, password }, { validateStatus: () => true });
  if (res.status === 200) {
    const data = parseOrThrow(AuthResponseSchema, res.data);
    const session = sessionFrom(data, username);
    if (session) {
      logger.info('AUTH', 'Recover success', { username });
      return session;
    }
  }
  const data = AuthResponseSchema.safeParse(res.data).data;
  logger.warn('AUTH', `Recover failed HTTP ${res.status}`, { username });
  throw new ApiError(data?.message ?? 'Окно восстановления истекло', res.status);
}
