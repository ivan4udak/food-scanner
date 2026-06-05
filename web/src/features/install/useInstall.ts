import { useCallback, useEffect, useState } from 'react';

const DISMISS_KEY = 'foodscanner.install.dismissed';
const DISMISS_MS = 24 * 60 * 60 * 1000; // 24 часа

/** Отложена ли установка и ещё не истёк 24-часовой период. */
export function isDismissed(): boolean {
  try {
    const raw = localStorage.getItem(DISMISS_KEY);
    if (!raw) return false;
    const ts = Number(raw);
    return Number.isFinite(ts) && Date.now() - ts < DISMISS_MS;
  } catch {
    return false;
  }
}

function rememberDismiss() {
  try {
    localStorage.setItem(DISMISS_KEY, String(Date.now()));
  } catch {
    /* localStorage недоступен — не критично */
  }
}

export interface InstallState {
  /** Доступен ли нативный prompt установки (Android Chrome). */
  deferred: BeforeInstallPromptEvent | null;
  /** Пользователь отложил установку (в пределах 24ч). */
  dismissed: boolean;
  /** Запустить нативный диалог установки (Android). */
  promptInstall: () => Promise<void>;
  /** Отложить установку на 24 часа. */
  dismiss: () => void;
  /** Снова показать экран установки (сбросить отложку). */
  reopen: () => void;
}

export function useInstall(): InstallState {
  const [deferred, setDeferred] = useState<BeforeInstallPromptEvent | null>(null);
  const [dismissed, setDismissed] = useState<boolean>(() => isDismissed());

  useEffect(() => {
    const onPrompt = (e: BeforeInstallPromptEvent) => {
      e.preventDefault(); // не показывать встроенную мини-плашку
      setDeferred(e);
    };
    const onInstalled = () => setDeferred(null);
    window.addEventListener('beforeinstallprompt', onPrompt);
    window.addEventListener('appinstalled', onInstalled);
    return () => {
      window.removeEventListener('beforeinstallprompt', onPrompt);
      window.removeEventListener('appinstalled', onInstalled);
    };
  }, []);

  const promptInstall = useCallback(async () => {
    if (!deferred) return;
    await deferred.prompt();
    await deferred.userChoice.catch(() => undefined);
    setDeferred(null);
  }, [deferred]);

  const dismiss = useCallback(() => {
    rememberDismiss();
    setDismissed(true);
  }, []);

  const reopen = useCallback(() => {
    try {
      localStorage.removeItem(DISMISS_KEY);
    } catch {
      /* ignore */
    }
    setDismissed(false);
  }, []);

  return { deferred, dismissed, promptInstall, dismiss, reopen };
}
