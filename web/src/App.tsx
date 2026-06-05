import { useEffect } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { RouterProvider } from 'react-router-dom';
import { ConnectionProvider } from '@/components/ConnectionContext';
import { router } from '@/router';
import { isIOS, isStandalone } from '@/lib/platform';
import { useInstall } from '@/features/install/useInstall';
import { InstallInstructionsPage } from '@/features/install/InstallInstructionsPage';
import { InstallReminderBanner } from '@/features/install/InstallReminderBanner';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, refetchOnWindowFocus: false, staleTime: 30_000 },
    mutations: { retry: 0 },
  },
});

export default function App() {
  const standalone = isStandalone();
  const ios = isIOS();
  const { deferred, dismissed, promptInstall, dismiss, reopen } = useInstall();

  // iPhone в обычной вкладке Safari и установка не отложена → обязательный экран установки.
  const showInstallPage = ios && !standalone && !dismissed;
  const iosReminder = ios && !standalone && dismissed;
  const showBanner = !standalone && !showInstallPage && (iosReminder || Boolean(deferred));

  // Пока показана нижняя плашка — резервируем место снизу, чтобы она не перекрывала
  // контент (например, кнопку ручного ввода ШК).
  useEffect(() => {
    document.body.classList.toggle('install-banner-open', showBanner);
    return () => document.body.classList.remove('install-banner-open');
  }, [showBanner]);

  if (showInstallPage) {
    return <InstallInstructionsPage deferred={deferred} onInstall={promptInstall} onDismiss={dismiss} />;
  }

  return (
    <QueryClientProvider client={queryClient}>
      <ConnectionProvider>
        {showBanner && (
          <InstallReminderBanner
            iosReminder={iosReminder}
            deferred={deferred}
            onInstall={promptInstall}
            onReopen={reopen}
          />
        )}
        <RouterProvider router={router} />
      </ConnectionProvider>
    </QueryClientProvider>
  );
}
