import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { adminUser, adminUserErrors } from '@/api/admin';
import { LogTable } from '@/features/admin/LogTable';

/** Страница ошибок (WARN/ERROR) конкретного пользователя. */
export function AdminUserErrorsPage() {
  const { id = '' } = useParams();
  const user = useQuery({ queryKey: ['admin-user', id], queryFn: () => adminUser(id) });
  const errors = useQuery({ queryKey: ['admin-user-errors', id], queryFn: () => adminUserErrors(id, 200) });

  return (
    <div className="card">
      <h2>Ошибки клиента{user.data ? ` — ${user.data.user.username}` : ''}</h2>
      {errors.isLoading ? (
        <p className="muted center">Загрузка…</p>
      ) : (
        <LogTable logs={errors.data ?? []} />
      )}
    </div>
  );
}
