import { useState, type FormEvent } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { recover } from '@/api/auth';
import { useAuthStore } from '@/store/authStore';
import { ApiError } from '@/api/client';
import { Page, TopBar } from '@/components/Layout';

/** Новый пароль в окне восстановления (5 мин после админ-сброса). */
export function RecoverPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const signIn = useAuthStore((s) => s.signIn);

  const [username, setUsername] = useState(params.get('u') ?? '');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (password.length < 4) return setError('Пароль не короче 4 символов');
    if (password !== confirm) return setError('Пароли не совпадают');
    setBusy(true);
    setError(null);
    try {
      const session = await recover(username.trim(), password);
      signIn(session);
      navigate('/', { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Окно восстановления истекло');
    } finally {
      setBusy(false);
    }
  }

  return (
    <Page>
      <TopBar title="Новый пароль" back />
      <p>Пароль был сброшен администратором. Задайте новый в течение окна восстановления.</p>
      <form onSubmit={handleSubmit} className="card stack">
        <div className="field">
          <label>Логин</label>
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoCapitalize="none" required />
        </div>
        <div className="field">
          <label>Новый пароль</label>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" required />
        </div>
        <div className="field">
          <label>Повторите пароль</label>
          <input type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} autoComplete="new-password" required />
        </div>
        {error && <div className="error">{error}</div>}
        <button className="btn" type="submit" disabled={busy || !username || !password}>
          {busy ? '…' : 'Сохранить пароль'}
        </button>
      </form>
    </Page>
  );
}
