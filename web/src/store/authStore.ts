import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Session } from '@/api/types';
import { logger } from '@/logging/logger';
import { isAdminToken, isSuperAdminToken } from '@/lib/jwt';

/**
 * Сессия: профиль контрибьютора + токены (access/refresh).
 * Persist в localStorage — PWA не имеет Keychain, но это стандарт для web-клиента.
 * Доступ вне React — через `useAuthStore.getState()` (используется axios-клиентом).
 */
interface AuthState {
  contributorId: string | null;
  username: string | null;
  accessToken: string | null;
  refreshToken: string | null;

  isAuthenticated: () => boolean;
  isAdmin: () => boolean;
  isSuperAdmin: () => boolean;
  signIn: (session: Session) => void;
  setTokens: (accessToken: string, refreshToken: string) => void;
  signOut: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      contributorId: null,
      username: null,
      accessToken: null,
      refreshToken: null,

      isAuthenticated: () => Boolean(get().accessToken && get().contributorId),
      isAdmin: () => isAdminToken(get().accessToken),
      isSuperAdmin: () => isSuperAdminToken(get().accessToken),

      signIn: (s) => {
        logger.info('AUTH', 'Session established', { contributorId: s.contributorId, username: s.username });
        set({
          contributorId: s.contributorId,
          username: s.username,
          accessToken: s.accessToken,
          refreshToken: s.refreshToken,
        });
      },

      setTokens: (accessToken, refreshToken) => set({ accessToken, refreshToken }),

      signOut: () => {
        logger.info('AUTH', 'Logout');
        set({ contributorId: null, username: null, accessToken: null, refreshToken: null });
      },
    }),
    { name: 'fs-auth' },
  ),
);
