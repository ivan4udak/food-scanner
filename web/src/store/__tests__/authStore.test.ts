import { describe, it, expect, beforeEach } from 'vitest';
import { useAuthStore } from '@/store/authStore';
import type { Session } from '@/api/types';

const session: Session = {
  contributorId: crypto.randomUUID(),
  username: 'alice',
  accessToken: 'access-1',
  refreshToken: 'refresh-1',
};

describe('authStore', () => {
  beforeEach(() => {
    useAuthStore.getState().signOut();
  });

  it('signIn сохраняет профиль и токены, isAuthenticated=true', () => {
    useAuthStore.getState().signIn(session);
    const s = useAuthStore.getState();
    expect(s.username).toBe('alice');
    expect(s.accessToken).toBe('access-1');
    expect(s.isAuthenticated()).toBe(true);
  });

  it('setTokens обновляет только токены (ротация refresh)', () => {
    useAuthStore.getState().signIn(session);
    useAuthStore.getState().setTokens('access-2', 'refresh-2');
    const s = useAuthStore.getState();
    expect(s.accessToken).toBe('access-2');
    expect(s.refreshToken).toBe('refresh-2');
    expect(s.username).toBe('alice');
  });

  it('signOut очищает сессию', () => {
    useAuthStore.getState().signIn(session);
    useAuthStore.getState().signOut();
    const s = useAuthStore.getState();
    expect(s.accessToken).toBeNull();
    expect(s.contributorId).toBeNull();
    expect(s.isAuthenticated()).toBe(false);
  });

  it('без contributorId isAuthenticated=false', () => {
    useAuthStore.getState().setTokens('a', 'r');
    expect(useAuthStore.getState().isAuthenticated()).toBe(false);
  });
});
