import { useState, type FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { register } from '@/api/auth';
import { useAuthStore } from '@/store/authStore';
import { ApiError } from '@/api/client';
import { Page, TopBar } from '@/components/Layout';

export function RegisterPage() {
  const navigate = useNavigate();
  const signIn = useAuthStore((s) => s.signIn);

  const [username, setUsername] = useState('');
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
      const session = await register(username.trim(), password);
      signIn(session);
      navigate('/', { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Не удалось создать аккаунт');
    } finally {
      setBusy(false);
    }
  }

  return (
    <Page>
      <TopBar title="Создать аккаунт" back />
      <form onSubmit={handleSubmit} className="card stack">
        <div className="field">
          <label>Логин</label>
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoCapitalize="none" required />
        </div>
        <div className="field">
          <label>Пароль (4–100 символов)</label>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" required />
        </div>
        <div className="field">
          <label>Повторите пароль</label>
          <input type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} autoComplete="new-password" required />
        </div>
        {error && <div className="error">{error}</div>}
        <button className="btn" type="submit" disabled={busy || !username || !password}>
          {busy ? '…' : 'Создать аккаунт'}
        </button>
      </form>
      <p className="center">
        Уже есть аккаунт? <Link to="/login">Войти</Link>
      </p>
    </Page>
  );
}
