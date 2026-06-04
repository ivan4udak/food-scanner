import { useMutation, useQuery } from '@tanstack/react-query';
import * as catalog from '@/api/catalog';
import { health } from '@/api/health';

/** GET /entries/{barcode} — null при 404. Кэшируется (+ SW offline). */
export const useEntryQuery = (barcode: string, enabled = true) =>
  useQuery({
    queryKey: ['entry', barcode],
    queryFn: () => catalog.getEntry(barcode),
    enabled: enabled && barcode.length > 0,
  });

/** GET /health — диагностика (Блок 20). */
export const useHealthQuery = () =>
  useQuery({
    queryKey: ['health'],
    queryFn: () => health(),
    staleTime: 0,
    refetchOnWindowFocus: true,
  });

/** POST /scan → { status, draftId }. */
export const useScanMutation = () =>
  useMutation({ mutationFn: (barcode: string) => catalog.scan(barcode) });

/** POST /drafts/{id}/complete. */
export const useCompleteMutation = () =>
  useMutation({ mutationFn: (draftId: string) => catalog.complete(draftId) });
