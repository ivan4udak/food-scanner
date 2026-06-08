import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from 'react';
import { ping } from '@/api/health';
import { logger } from '@/logging/logger';

export type ConnState = 'connecting' | 'online' | 'degraded' | 'offline';

const ConnectionContext = createContext<ConnState>('connecting');

/**
 * Один heartbeat-цикл на всё приложение (как ConnectionMonitor в iOS, Блок 5):
 * GET /ping каждые 5с. ONLINE (<10с) / DEGRADED (10–20с) / OFFLINE (>20с).
 */
export function ConnectionProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<ConnState>('connecting');
  const lastSuccess = useRef<number | null>(null);
  const startedAt = useRef<number>(Date.now());
  const prevState = useRef<ConnState>('connecting');
  const enteredAt = useRef<number>(Date.now());

  useEffect(() => {
    let cancelled = false;

    const apply = (next: ConnState) => {
      if (next !== prevState.current) {
        const heldSec = Math.round((Date.now() - enteredAt.current) / 1000);
        logger.info('NETWORK', `${next.toUpperCase()} after ${heldSec}s in ${prevState.current.toUpperCase()}`);
        prevState.current = next;
        enteredAt.current = Date.now();
      }
      setState(next);
    };

    const recompute = () => {
      const now = Date.now();
      if (lastSuccess.current != null) {
        const elapsed = (now - lastSuccess.current) / 1000;
        apply(elapsed < 10 ? 'online' : elapsed < 20 ? 'degraded' : 'offline');
      } else {
        apply((now - startedAt.current) / 1000 < 20 ? 'connecting' : 'offline');
      }
    };

    const tick = async () => {
      const ok = await ping();
      if (cancelled) return;
      if (ok) lastSuccess.current = Date.now();
      recompute();
    };

    void tick();
    const id = window.setInterval(tick, 5000);
    const onVisible = () => {
      if (document.visibilityState === 'visible') void tick();
    };
    document.addEventListener('visibilitychange', onVisible);

    return () => {
      cancelled = true;
      window.clearInterval(id);
      document.removeEventListener('visibilitychange', onVisible);
    };
  }, []);

  return <ConnectionContext.Provider value={state}>{children}</ConnectionContext.Provider>;
}

export const useConnection = () => useContext(ConnectionContext);

export function connectionLabel(state: ConnState): string {
  switch (state) {
    case 'online':
      return 'В сети';
    case 'degraded':
      return 'Нестабильно';
    case 'offline':
      return 'Нет связи';
    default:
      return 'Подключение…';
  }
}
