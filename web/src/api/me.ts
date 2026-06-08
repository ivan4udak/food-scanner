import { api } from '@/api/client';

export interface MyScanRow {
  barcode: string;
  scanStatus: 'DRAFT_OPEN' | 'COMPLETED' | string;
  catalogEntryId: string | null;
  firstScannedAt: string | null;
  completedAt: string | null;
  photoCount: number;
  ocrStatus: string | null;
}

export interface MyScanPhoto {
  id: string; type: string; storageKey: string;
  thumbUrl: string; fullUrl: string; capturedAt: string | null;
}

export interface MyScanDetail {
  barcode: string;
  catalogEntryId: string | null;
  firstScannedAt: string | null;
  completedAt: string | null;
  photos: MyScanPhoto[];
  ocrStatus: string | null;
}

export const getMyScans = async (): Promise<MyScanRow[]> => (await api.get('/me/scans')).data;
export const getMyScanDetail = async (barcode: string): Promise<MyScanDetail> =>
  (await api.get(`/me/scans/${encodeURIComponent(barcode)}`)).data;
