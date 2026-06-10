import { api } from '@/api/client';

export interface AdminDashboard {
  usersTotal: number; onlineNow: number; activeToday: number; activeWeek: number;
  scansToday: number; scansWeek: number; entriesToday: number; entriesWeek: number;
  photosToday: number; clientErrorsToday: number; serverErrorsToday: number;
}

export interface AdminUserRow {
  id: string; username: string; role: string; online: boolean; lastActivityAt: string | null;
  clientVersion: string | null; browser: string | null; os: string | null; deviceType: string | null;
  totalScans: number; completedEntries: number; uploadedPhotos: number; clientErrors: number;
}

export interface AdminSessionRow {
  sessionId: string; startedAt: string; lastSeenAt: string; clientVersion: string | null;
  browser: string | null; os: string | null; deviceType: string | null;
  networkStatus: string | null; standalone: boolean | null;
}

export interface AdminScanRow {
  barcode: string; status: string; draftId: string | null; catalogEntryId: string | null;
  firstScannedAt: string | null; completedAt: string | null; photoCount: number;
}

export interface AdminUserDetail {
  user: AdminUserRow; sessions: AdminSessionRow[]; recentScans: AdminScanRow[];
}

export interface AdminClientLog {
  id: string; contributorId: string | null; username: string | null; sessionId: string | null;
  correlationId: string | null; timestamp: string; level: string; category: string;
  event: string | null; screen: string | null; message: string | null; metadataJson: string | null;
  durationMs: number | null; stackTrace: string | null; barcode: string | null;
  apiMethod: string | null; apiPath: string | null; httpStatus: number | null;
}

export interface AdminServerEventRow {
  id: string; occurredAt: string; level: string; event: string; correlationId: string | null;
  contributorId: string | null; username: string | null; method: string | null; path: string | null;
  httpStatus: number | null; durationMs: number | null; useCase: string | null; barcode: string | null;
  errorCode: string | null; errorMessage: string | null; exceptionClass: string | null;
}

export interface AdminCatalogRow {
  catalogEntryId: string; barcode: string; contributorId: string | null;
  author: string | null; createdAt: string; photoCount: number;
}

export interface AdminCatalogDetail {
  catalogEntryId: string; barcode: string; contributorId: string | null; author: string | null;
  createdAt: string;
  photos: { id: string; type: string; storageKey: string; capturedAt: string | null }[];
  relatedLogs: AdminClientLog[];
}

export interface TraceItem {
  source: 'CLIENT' | 'SERVER'; at: string; level: string | null; category: string | null;
  event: string | null; message: string | null; method: string | null; path: string | null;
  httpStatus: number | null; durationMs: number | null;
}

export interface AdminErrors { client: AdminClientLog[]; server: AdminServerEventRow[]; }

export interface LogFilters {
  contributorId?: string; sessionId?: string; level?: string; category?: string;
  event?: string; barcode?: string; screen?: string; dateFrom?: string; dateTo?: string;
  limit?: number; offset?: number;
}

const get = async <T>(url: string, params?: Record<string, unknown>): Promise<T> =>
  (await api.get(url, { params })).data as T;

export const adminDashboard = () => get<AdminDashboard>('/admin/dashboard');
export const adminUsers = (sort?: string, limit = 100, offset = 0) =>
  get<AdminUserRow[]>('/admin/users', { sort, limit, offset });
export const adminUser = (id: string) => get<AdminUserDetail>(`/admin/users/${id}`);
export const adminUserByName = (username: string) =>
  get<AdminUserDetail>(`/admin/users/by-username/${encodeURIComponent(username)}`);
export const adminUserLogs = (id: string, limit = 200) =>
  get<AdminClientLog[]>(`/admin/users/${id}/logs`, { limit });
export const adminUserErrors = (id: string, limit = 200) =>
  get<AdminClientLog[]>(`/admin/users/${id}/errors`, { limit });
export const adminLogs = (f: LogFilters) => get<AdminClientLog[]>('/admin/logs', f as Record<string, unknown>);
export const adminErrors = (limit = 200) => get<AdminErrors>('/admin/errors', { limit });
export const adminCatalog = (limit = 100, offset = 0) =>
  get<AdminCatalogRow[]>('/admin/catalog', { limit, offset });
export const adminCatalogDetail = (barcode: string) =>
  get<AdminCatalogDetail>(`/admin/catalog/${encodeURIComponent(barcode)}`);
export const adminTrace = (correlationId: string) => get<TraceItem[]>(`/admin/trace/${correlationId}`);

export interface AdminOcrRow {
  jobId: string; barcode: string | null; draftId: string | null; catalogEntryId: string | null;
  photoType: string; storageKey: string; statusCode: number; status: string; attempts: number;
  updatedAt: string | null; errorCode: string | null; errorMessage: string | null;
  rawTextPreview: string | null;
}

export interface AdminOcrSummary {
  total: number;
  byStatus: { code: number; status: string; count: number }[];
}

export const adminOcr = (status?: number, barcode?: string, limit = 100, offset = 0) =>
  get<AdminOcrRow[]>('/admin/ocr', { status, barcode, limit, offset });
export const adminOcrSummary = () => get<AdminOcrSummary>('/admin/ocr/summary');

/** Смена роли пользователя (только SUPER_ADMIN). → новая роль. */
export async function setUserRole(id: string, role: string): Promise<string> {
  const res = await api.post(`/admin/users/${id}/role`, { role });
  return (res.data?.role as string) ?? role;
}
