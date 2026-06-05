import type { ReactNode } from 'react';

interface Props {
  /** Нативный prompt установки (Android) — если доступен, показываем кнопку. */
  deferred: BeforeInstallPromptEvent | null;
  onInstall: () => void;
  onDismiss: () => void;
}

const STEPS: { icon: ReactNode; text: string }[] = [
  { icon: <ShareGlyph />, text: 'Нажмите «Поделиться» в Safari' },
  { icon: <PlusGlyph />, text: 'Выберите «На экран „Домой“»' },
  { icon: <span className="install-step-badge">Add</span>, text: 'Нажмите «Добавить»' },
  { icon: <HomeGlyph />, text: 'Запустите Food Scanner с экрана «Домой»' },
];

/** Обязательный экран установки для iPhone (в обычной вкладке Safari). */
export function InstallInstructionsPage({ deferred, onInstall, onDismiss }: Props) {
  return (
    <main className="install-page">
      <img src="/icon.svg" alt="Food Scanner" className="install-logo" />
      <h1 className="install-title">Установите Food Scanner</h1>
      <p className="install-desc">
        Для сканирования штрихкодов и доступа к камере установите приложение на экран «Домой».
      </p>

      <ol className="install-steps">
        {STEPS.map((s, i) => (
          <li className="install-step" key={i}>
            <span className="install-step-num">{i + 1}</span>
            <span className="install-step-icon">{s.icon}</span>
            <span className="install-step-text">{s.text}</span>
          </li>
        ))}
      </ol>

      {/* Заглушка-иллюстрация (скриншот добавится позже) */}
      <div className="install-illustration" aria-hidden="true">
        <span>📱 Safari → Поделиться → На экран «Домой»</span>
      </div>

      {deferred && (
        <button className="btn" onClick={onInstall}>
          Установить приложение
        </button>
      )}

      <button className="btn ghost" onClick={onDismiss}>
        Продолжить в браузере
      </button>
      <p className="install-note">Напоминание появится снова через 24 часа.</p>
    </main>
  );
}

function ShareGlyph() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 16V4" />
      <path d="M8 8l4-4 4 4" />
      <path d="M6 12v6a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2v-6" />
    </svg>
  );
}
function PlusGlyph() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <rect x="4" y="4" width="16" height="16" rx="4" />
      <path d="M12 9v6M9 12h6" />
    </svg>
  );
}
function HomeGlyph() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 11l9-8 9 8" />
      <path d="M5 10v10h14V10" />
    </svg>
  );
}
