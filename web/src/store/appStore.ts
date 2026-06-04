import { create } from 'zustand';
import { persist } from 'zustand/middleware';

/** Источник фото: камера (getUserMedia/capture) или файл/галерея. */
export type PhotoSource = 'camera' | 'file';

interface AppState {
  /** Режим загрузки фото по умолчанию (как «шестерёнка» в iOS, Блоки 11–12). */
  photoSource: PhotoSource | null;
  setPhotoSource: (s: PhotoSource | null) => void;
}

export const useAppStore = create<AppState>()(
  persist(
    (set) => ({
      photoSource: null,
      setPhotoSource: (photoSource) => set({ photoSource }),
    }),
    { name: 'fs-app' },
  ),
);
