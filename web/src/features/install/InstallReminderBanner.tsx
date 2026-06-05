interface Props {
  /** iOS не-standalone и установка отложена — предлагаем вернуть инструкцию. */
  iosReminder: boolean;
  /** Android: доступен нативный prompt установки. */
  deferred: BeforeInstallPromptEvent | null;
  onInstall: () => void;
  onReopen: () => void;
}

/** Маленькая плашка-напоминание об установке (когда полноэкранный экран скрыт). */
export function InstallReminderBanner({ iosReminder, deferred, onInstall, onReopen }: Props) {
  if (deferred) {
    return (
      <div className="install-banner">
        <span>Установить Food Scanner на устройство</span>
        <button className="btn" style={{ width: 'auto' }} onClick={onInstall}>
          Установить
        </button>
      </div>
    );
  }
  if (iosReminder) {
    return (
      <div className="install-banner">
        <span>Для камеры добавьте приложение на экран «Домой»</span>
        <button className="btn secondary" style={{ width: 'auto' }} onClick={onReopen}>
          Как установить
        </button>
      </div>
    );
  }
  return null;
}
