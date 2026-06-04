import { createBrowserRouter, Navigate, Outlet } from 'react-router-dom';
import { ConnectionBanner } from '@/components/ConnectionBanner';
import { ProtectedRoute } from '@/components/ProtectedRoute';
import { LoginPage } from '@/features/auth/LoginPage';
import { RegisterPage } from '@/features/auth/RegisterPage';
import { RecoverPage } from '@/features/auth/RecoverPage';
import { ScanPage } from '@/features/scan/ScanPage';
import { DraftPage } from '@/features/draft/DraftPage';
import { LookupPage } from '@/features/lookup/LookupPage';
import { CompletedPage } from '@/features/result/CompletedPage';
import { AboutPage } from '@/features/about/AboutPage';

function RootLayout() {
  return (
    <>
      <ConnectionBanner />
      <Outlet />
    </>
  );
}

const protect = (el: React.ReactNode) => <ProtectedRoute>{el}</ProtectedRoute>;

export const router = createBrowserRouter([
  {
    element: <RootLayout />,
    children: [
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      { path: '/recover', element: <RecoverPage /> },
      { path: '/', element: protect(<ScanPage />) },
      { path: '/draft/:draftId', element: protect(<DraftPage />) },
      { path: '/product/:barcode', element: protect(<LookupPage />) },
      { path: '/completed', element: protect(<CompletedPage />) },
      { path: '/about', element: protect(<AboutPage />) },
      { path: '*', element: <Navigate to="/" replace /> },
    ],
  },
]);
