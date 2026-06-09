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
import { PublicStatsPage } from '@/features/publicStats/PublicStatsPage';
import { AdminLayout } from '@/features/admin/AdminLayout';
import { AdminDashboardPage } from '@/features/admin/AdminDashboardPage';
import { AdminUsersPage } from '@/features/admin/AdminUsersPage';
import { AdminUserDetailPage } from '@/features/admin/AdminUserDetailPage';
import { AdminLogsPage } from '@/features/admin/AdminLogsPage';
import { AdminCatalogPage } from '@/features/admin/AdminCatalogPage';
import { AdminCatalogDetailPage } from '@/features/admin/AdminCatalogDetailPage';
import { AdminErrorsPage } from '@/features/admin/AdminErrorsPage';
import { AdminTracePage } from '@/features/admin/AdminTracePage';
import { MyScansPage } from '@/features/myScans/MyScansPage';
import { MyScanDetailPage } from '@/features/myScans/MyScanDetailPage';

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
      { path: '/stats', element: <PublicStatsPage /> },
      { path: '/', element: protect(<ScanPage />) },
      { path: '/draft/:draftId', element: protect(<DraftPage />) },
      { path: '/product/:barcode', element: protect(<LookupPage />) },
      { path: '/completed', element: protect(<CompletedPage />) },
      { path: '/about', element: protect(<AboutPage />) },
      { path: '/my-scans', element: protect(<MyScansPage />) },
      { path: '/my-scans/:barcode', element: protect(<MyScanDetailPage />) },
      {
        path: '/admin',
        element: protect(<AdminLayout />),
        children: [
          { index: true, element: <AdminDashboardPage /> },
          { path: 'users', element: <AdminUsersPage /> },
          { path: 'users/:id', element: <AdminUserDetailPage /> },
          { path: 'logs', element: <AdminLogsPage /> },
          { path: 'catalog', element: <AdminCatalogPage /> },
          { path: 'catalog/:barcode', element: <AdminCatalogDetailPage /> },
          { path: 'errors', element: <AdminErrorsPage /> },
          { path: 'trace/:correlationId', element: <AdminTracePage /> },
        ],
      },
      { path: '*', element: <Navigate to="/" replace /> },
    ],
  },
]);
