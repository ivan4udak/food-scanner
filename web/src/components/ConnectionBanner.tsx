import { useEffect, useRef, useState } from 'react';
import { useConnection, connectionLabel, type ConnState } from '@/components/ConnectionContext';

/**
 * Баннер связи (как индикатор соединения iOS): жёлтый при degraded, красный при offline,
 * короткий зелёный «Связь восстановлена» при возврате online.
 */
export function ConnectionBanner() {
  const state = useConnection();
  const prev = useRef<ConnState>(state);
  const [reconnected, setReconnected] = useState(false);

  useEffect(() => {
    if (state === 'online' && prev.current !== 'online' && prev.current !== 'connecting') {
      setReconnected(true);
      const t = setTimeout(() => setReconnected(false), 1500);
      prev.current = state;
      return () => clearTimeout(t);
    }
    prev.current = state;
    return undefined;
  }, [state]);

  if (state === 'offline') return <div className="banner offline">Нет соединения с сервером</div>;
  if (state === 'degraded') return <div className="banner degraded">Соединение нестабильно</div>;
  if (reconnected) return <div className="banner online">Связь восстановлена</div>;
  return null;
}

export { connectionLabel };
