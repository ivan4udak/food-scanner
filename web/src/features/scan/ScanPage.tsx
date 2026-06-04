import { useCallback, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { BarcodeScanner } from '@/features/scan/BarcodeScanner';
import { useScanMutation } from '@/hooks/queries';
import { useConnection } from '@/components/ConnectionContext';
import { ApiError } from '@/api/client';
import { Page, TopBar } from '@/components/Layout';

export function ScanPage() {
  const navigate = useNavigate();
  const connection = useConnection();
  const scan = useScanMutation();
  const [manual, setManual] = useState('');
  const [error, setError] = useState<string | null>(null);

  const online = connection === 'online';
  const busy = scan.isPending;

  const handleBarcode = useCallback(
    (value: string) => {
      if (scan.isPending) return;
      setError(null);
      scan.mutate(value, {
        onSuccess: (res) => {
          if (res.status === 'NEW' && res.draftId) {
            navigate(`/draft/${res.draftId}?b=${encodeURIComponent(value)}`);
          } else {
            navigate(`/product/${encodeURIComponent(value)}`);
          }
        },
        onError: (e) => setError(e instanceof ApiError ? e.message : 'Ошибка сканирования'),
      });
    },
    [navigate, scan],
  );

  function submitManual(e: FormEvent) {
    e.preventDefault();
    const v = manual.trim();
    if (v) handleBarcode(v);
  }

  return (
    <Page>
      <TopBar
        title="Сканировать"
        right={
          <button className="iconbtn" aria-label="О приложении" onClick={() => navigate('/about')}>
            ⚙
          </button>
        }
      />

      <BarcodeScanner onDetected={handleBarcode} paused={!online || busy} />

      {!online && <p className="center">Сканирование доступно при подключении к серверу.</p>}
      {busy && <p className="center muted">Проверяем штрихкод…</p>}
      {error && <div className="error center">{error}</div>}

      <form onSubmit={submitManual} className="card stack">
        <div className="field">
          <label>Ввести штрихкод вручную</label>
          <input
            inputMode="numeric"
            value={manual}
            onChange={(e) => setManual(e.target.value)}
            placeholder="4607038310042"
          />
        </div>
        <button className="btn secondary" type="submit" disabled={busy || !manual.trim()}>
          Найти / создать
        </button>
      </form>
    </Page>
  );
}
