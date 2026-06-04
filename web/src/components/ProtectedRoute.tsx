import { Navigate, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useAuthStore } from '@/store/authStore';

/** Пускает только авторизованного; иначе → /login (с возвратом). */
export function ProtectedRoute({ children }: { children: ReactNode }) {
  const authed = useAuthStore((s) => Boolean(s.accessToken && s.contributorId));
  const location = useLocation();
  if (!authed) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  return <>{children}</>;
}
