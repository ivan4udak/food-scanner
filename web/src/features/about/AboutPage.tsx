import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useHealthQuery } from '@/hooks/queries';
import { useConnection, connectionLabel } from '@/components/ConnectionContext';
import { useAuthStore } from '@/store/authStore';
import { API_BASE } from '@/api/client';
import { APP_VERSION, PLATFORM } from '@/version';
import { Page, TopBar } from '@/components/Layout';
import { logger, formatLogLine } from '@/logging/logger';
import { browserInfo, buildDiagnosticsText, downloadLog } from '@/logging/diagnostics';
import { flushTelemetry } from '@/logging/telemetry';
import { setLeaderboardVisibility } from '@/api/stats';

/** Экран «О приложении» + диагностический пакет (паритет с iOS, Блок 20). */
export function AboutPage() {
  const navigate = useNavigate();
  const connection = useConnection();
  const { data: health, isLoading: healthLoading } = useHealthQuery();
  const { contributorId, username, signOut, isAdmin } = useAuthStore();

  const [cacheSize, setCacheSize] = useState<number | null>(null);
  const [copied, setCopied] = useState(false);
  const [showLogs, setShowLogs] = useState(false);
  const [logTick, setLogTick] = useState(0); // принудительное обновление списка логов
  const [hiddenFromBoard, setHiddenFromBoard] = useState<boolean | null>(null);
  const [boardBusy, setBoardBusy] = useState(false);

  async function toggleLeaderboard(next: boolean) {
    setBoardBusy(true);
    try {
      const result = await setLeaderboardVisibility(next);
      setHiddenFromBoard(result);
    } catch {
      /* состояние не меняем */
    } finally {
      setBoardBusy(false);
    }
  }

  function openLogs() {
    flushTelemetry(); // досылаем накопленное на сервер при открытии диагностики
    setShowLogs((v) => !v);
    setLogTick((t) => t + 1);
  }

  useEffect(() => {
    navigator.storage?.estimate?.().then((e) => setCacheSize(e.usage ?? 0)).catch(() => setCacheSize(null));
  }, []);

  const connText = connectionLabel(connection);
  const logCount = logger.count();
  const lastLogs = showLogs ? logger.recent(100) : [];
  void logTick; // зависимость для пересборки списка по кнопке «Обновить»

  const backendText = healthLoading ? 'Проверка…' : health ? 'Работает' : 'Недоступен';
  const storageText = healthLoading ? 'Проверка…' : !health ? '—' : health.storage === 'UP' ? 'Работает' : 'Недоступно';
  const storageDot = !health ? 'connecting' : health.storage === 'UP' ? 'online' : 'offline';
  const backendDot = healthLoading ? 'connecting' : health ? 'online' : 'offline';

  const cacheText = cacheSize == null ? '—' : formatBytes(cacheSize);

  async function copyDiagnostics() {
    try {
      await navigator.clipboard.writeText(buildDiagnosticsText(connText, 500));
      navigator.vibrate?.(10);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      setCopied(false);
    }
  }

  async function clearCache() {
    if ('caches' in window) {
      const keys = await caches.keys();
      await Promise.all(keys.map((k) => caches.delete(k)));
    }
    navigator.storage?.estimate?.().then((e) => setCacheSize(e.usage ?? 0));
  }

  function logout() {
    signOut();
    navigate('/login', { replace: true });
  }

  return (
    <Page>
      <TopBar title="О приложении" back />

      <div className="card">
        <h2>Версии</h2>
        <div className="row"><span>Платформа</span><span className="value">{PLATFORM}</span></div>
        <div className="row"><span>Версия</span><span className="value">{APP_VERSION}</span></div>
      </div>

      <div className="card">
        <h2>Сервер и связь</h2>
        <div className="row"><span>Base API</span><span className="value">{location.origin}{API_BASE}</span></div>
        <div className="row"><span>Связь</span><span className="value"><span className={`dot ${connection}`} /> {connectionLabel(connection)}</span></div>
        <div className="row"><span>Backend</span><span className="value"><span className={`dot ${backendDot}`} /> {backendText}</span></div>
        <div className="row"><span>Хранилище (MinIO)</span><span className="value"><span className={`dot ${storageDot}`} /> {storageText}</span></div>
      </div>

      <div className="card">
        <h2>Кэш</h2>
        <div className="row"><span>Занято</span><span className="value">{cacheText}</span></div>
        <button className="btn secondary" onClick={clearCache}>Очистить кэш</button>
      </div>

      <div className="card">
        <h2>Аккаунт</h2>
        <div className="row"><span>Логин</span><span className="value">{username ?? '—'}</span></div>
        <div className="row"><span>Contributor ID</span><span className="value">{contributorId ?? '—'}</span></div>
        <div className="row">
          <span>Скрыть меня из рейтинга</span>
          <span className="value">
            <button
              className={`btn ${hiddenFromBoard ? 'secondary' : 'ghost'}`}
              style={{ width: 'auto', padding: '8px 14px' }}
              disabled={boardBusy}
              onClick={() => toggleLeaderboard(!hiddenFromBoard)}
            >
              {hiddenFromBoard ? 'Скрыт' : 'Виден'}
            </button>
          </span>
        </div>
        <button className="btn secondary" onClick={() => navigate('/my-scans')}>Мои сканы →</button>
        <button className="btn ghost" onClick={() => navigate('/stats')}>Публичная статистика →</button>
        {isAdmin() && (
          <button className="btn secondary" onClick={() => navigate('/admin')}>Админ-панель →</button>
        )}
      </div>

      <div className="card">
        <h2>Диагностика</h2>
        <div className="row"><span>Версия</span><span className="value">{APP_VERSION} ({PLATFORM})</span></div>
        <div className="row"><span>Браузер</span><span className="value">{browserInfo()}</span></div>
        <div className="row"><span>Backend URL</span><span className="value">{location.origin}{API_BASE}</span></div>
        <div className="row"><span>Состояние связи</span><span className="value"><span className={`dot ${connection}`} /> {connText}</span></div>
        <div className="row"><span>Записей в логе</span><span className="value">{logCount}</span></div>

        <div className="stack" style={{ marginTop: 12 }}>
          <button className="btn secondary" onClick={openLogs}>
            {showLogs ? 'Скрыть лог' : 'Показать последние 100'}
          </button>
          {showLogs && (
            <>
              <button className="btn ghost" onClick={() => setLogTick((t) => t + 1)}>Обновить</button>
              <div className="logview">
                {lastLogs.length === 0
                  ? <div className="muted">Лог пуст.</div>
                  : lastLogs.map((e, i) => (
                      <div key={i} className={`logline lv-${e.level}`}>{formatLogLine(e)}</div>
                    ))}
              </div>
            </>
          )}
        </div>
      </div>

      <button className="btn" onClick={copyDiagnostics}>
        {copied ? 'Скопировано ✓' : 'Скопировать диагностику'}
      </button>
      <button className="btn secondary" onClick={() => downloadLog(connText)}>Скачать лог</button>
      <button className="btn danger" onClick={logout}>Выйти из аккаунта</button>
    </Page>
  );
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} Б`;
  const units = ['КБ', 'МБ', 'ГБ'];
  let v = bytes / 1024;
  let i = 0;
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024;
    i += 1;
  }
  return `${v.toFixed(1)} ${units[i]}`;
}
