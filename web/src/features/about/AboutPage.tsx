import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useHealthQuery } from '@/hooks/queries';
import { useConnection, connectionLabel } from '@/components/ConnectionContext';
import { useAuthStore } from '@/store/authStore';
import { API_BASE } from '@/api/client';
import { APP_VERSION, PLATFORM } from '@/version';
import { Page, TopBar } from '@/components/Layout';

/** Экран «О приложении» + диагностический пакет (паритет с iOS, Блок 20). */
export function AboutPage() {
  const navigate = useNavigate();
  const connection = useConnection();
  const { data: health, isLoading: healthLoading } = useHealthQuery();
  const { contributorId, username, signOut } = useAuthStore();

  const [cacheSize, setCacheSize] = useState<number | null>(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    navigator.storage?.estimate?.().then((e) => setCacheSize(e.usage ?? 0)).catch(() => setCacheSize(null));
  }, []);

  const backendText = healthLoading ? 'Проверка…' : health ? 'Работает' : 'Недоступен';
  const storageText = healthLoading ? 'Проверка…' : !health ? '—' : health.storage === 'UP' ? 'Работает' : 'Недоступно';
  const storageDot = !health ? 'connecting' : health.storage === 'UP' ? 'online' : 'offline';
  const backendDot = healthLoading ? 'connecting' : health ? 'online' : 'offline';

  const cacheText = cacheSize == null ? '—' : formatBytes(cacheSize);

  function buildDiagnostics(): string {
    return [
      'Food Scanner — диагностика',
      `Время: ${new Date().toISOString()}`,
      `Платформа: ${PLATFORM}`,
      `Версия: ${APP_VERSION}`,
      `User-Agent: ${navigator.userAgent}`,
      `Base API: ${location.origin}${API_BASE}`,
      `Связь: ${connectionLabel(connection)}`,
      `Backend: ${backendText}`,
      `Хранилище (MinIO): ${storageText}`,
      `Кэш: ${cacheText}`,
      `Логин: ${username ?? '—'}`,
      `Contributor ID: ${contributorId ?? '—'}`,
    ].join('\n');
  }

  async function copyDiagnostics() {
    try {
      await navigator.clipboard.writeText(buildDiagnostics());
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
      </div>

      <button className="btn" onClick={copyDiagnostics}>
        {copied ? 'Скопировано ✓' : 'Скопировать диагностику'}
      </button>
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
