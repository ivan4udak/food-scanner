import { describe, it, expect } from 'vitest';
import { roleFromToken, isAdminToken } from '@/lib/jwt';

function token(payload: object): string {
  return `h.${btoa(JSON.stringify(payload))}.s`;
}

describe('jwt role', () => {
  it('читает роль из payload', () => {
    expect(roleFromToken(token({ role: 'ADMIN' }))).toBe('ADMIN');
    expect(roleFromToken(token({ role: 'user' }))).toBe('USER');
  });

  it('isAdminToken различает админа', () => {
    expect(isAdminToken(token({ role: 'ADMIN' }))).toBe(true);
    expect(isAdminToken(token({ role: 'SUPER_ADMIN' }))).toBe(true);
    expect(isAdminToken(token({ role: 'USER' }))).toBe(false);
  });

  it('пустой/битый токен → USER, не админ', () => {
    expect(roleFromToken(null)).toBe('USER');
    expect(roleFromToken('garbage')).toBe('USER');
    expect(isAdminToken(null)).toBe(false);
  });
});
