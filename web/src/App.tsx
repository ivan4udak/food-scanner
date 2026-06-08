import { useEffect } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { RouterProvider } from 'react-router-dom';
import { ConnectionProvider } from '@/components/ConnectionContext';
import { router } from '@/router';
import { isIOS, isStandalone } from '@/lib/platform';
import { useInstall } from '@/features/install/useInstall';
import { InstallInstructionsPage } from '@/features/install/InstallInstructionsPage';
import { InstallReminderBanner } from '@/features/install/InstallReminderBanner';
import { logger } from '@/logging/logger';
import { APP_VERSION } from '@/version';

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

  // Стартовый лог: версия + режим запуска.
  useEffect(() => {
    logger.info('SYSTEM', `App start v${APP_VERSION}`, { standalone, ios });
    if (standalone) logger.info('PWA', 'Standalone mode');
  }, [standalone, ios]);

  useEffect(() => {
    if (showInstallPage) logger.info('PWA', 'Install instructions shown');
  }, [showInstallPage]);

  // Пока показана нижняя плашка — резервируем место снизу, чтобы она не перекрывала
  // контент (например, кнопку ручного ввода ШК).
  useEffect(() => {
    document.body.classList.toggle('install-banner-open', showBanner);
    if (showBanner) logger.info('PWA', 'Install banner shown');
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
