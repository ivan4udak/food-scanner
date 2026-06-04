import { useState, type FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { login, register } from '@/api/auth';
import { useAuthStore } from '@/store/authStore';
import { ApiError } from '@/api/client';
import { Page } from '@/components/Layout';

export function LoginPage() {
  const navigate = useNavigate();
  const signIn = useAuthStore((s) => s.signIn);

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [mode, setMode] = useState<'login' | 'create'>('login');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function handleLogin(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const out = await login(username.trim(), password);
      switch (out.kind) {
        case 'ok':
          signIn(out.session);
          navigate('/', { replace: true });
          break;
        case 'recovery':
          navigate(`/recover?u=${encodeURIComponent(out.username)}`);
          break;
        case 'notFound':
          setMode('create');
          setError('Пользователь не найден. Создайте аккаунт.');
          break;
        case 'invalid':
        case 'locked':
          setError(out.message);
          break;
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Ошибка входа');
    } finally {
      setBusy(false);
    }
  }

  async function handleCreate(e: FormEvent) {
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
      <div className="grow" />
      <h1>Food Scanner</h1>
      <p>{mode === 'login' ? 'Войдите, чтобы продолжить.' : 'Новый аккаунт.'}</p>

      <form onSubmit={mode === 'login' ? handleLogin : handleCreate} className="card stack">
        <div className="field">
          <label>Логин</label>
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoCapitalize="none" autoComplete="username" required />
        </div>
        <div className="field">
          <label>Пароль</label>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} required />
        </div>
        {mode === 'create' && (
          <div className="field">
            <label>Повторите пароль</label>
            <input type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} autoComplete="new-password" required />
          </div>
        )}

        {error && <div className="error">{error}</div>}

        <button className="btn" type="submit" disabled={busy || !username || !password}>
          {busy ? '…' : mode === 'login' ? 'Войти' : 'Создать аккаунт'}
        </button>

        {mode === 'create' && (
          <button type="button" className="btn ghost" onClick={() => { setMode('login'); setError(null); }}>
            Назад ко входу
          </button>
        )}
      </form>

      {mode === 'login' && (
        <p className="center">
          Нет аккаунта? <Link to="/register">Создать</Link>
        </p>
      )}
      <div className="grow" />
    </Page>
  );
}
