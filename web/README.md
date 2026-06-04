# Food Scanner — PWA frontend

Устанавливаемое PWA-приложение, заменяющее SwiftUI iOS-клиент. Работает с
существующим backend (`/api/v1`, Spring Boot) **без изменений API**.

## Стек
- React 18 + TypeScript 5 + Vite 5
- `vite-plugin-pwa` (Workbox) — manifest, service worker, offline-кэш
- TanStack Query 5 — серверное состояние, кэш, ретраи
- React Router 6 — маршрутизация SPA
- Zustand — сессия (`authStore`) и UI-состояние (`appStore`)
- Axios — клиент с авто-`refresh` access-токена и повтором на `401`
- Zod — валидация ответов API (`safeParse`) → нет «тихих» падений
- BarcodeDetector API + fallback на `@zxing/browser`
- `browser-image-compression` — сжатие фото перед загрузкой

## Функционал (паритет с iOS-клиентом)
Login · Register · Recover Password · Scan Barcode · Draft · Upload Photos ·
Complete Catalog · Lookup Product · About + Diagnostics.

## Структура
```
src/
  api/        types.ts (zod) · client.ts (axios+refresh) · auth · catalog · health
  store/      authStore · appStore (zustand persist)
  lib/        barcode.ts (BarcodeDetector→ZXing) · imageCompression.ts (+EXIF)
  hooks/      queries.ts (TanStack) · components/ConnectionContext (ping 5с)
  components/ ProtectedRoute · ConnectionBanner · AuthedImage · Spinner · Layout
  features/   auth/ scan/ draft/ result/ lookup/ about/
  router.tsx · App.tsx · main.tsx
```

## Запуск (dev)
```bash
npm install
# backend должен быть доступен (по умолчанию http://localhost:8080)
VITE_API_TARGET=http://localhost:8080 npm run dev   # http://localhost:5173
```
Vite проксирует `/api` на backend (один origin → нет CORS, работает Bearer).

## Тесты и сборка
```bash
npm test          # vitest (api/zod, refresh, stores, barcode, image)
npm run build     # tsc + vite build (+ генерация SW/manifest)
npm run preview
```

## Docker (прод)
```bash
docker compose up -d --build           # http://localhost:8081
# backend по умолчанию берётся с хоста: http://host.docker.internal:8080
# иной адрес:
BACKEND_URL=http://backend:8080 docker compose up -d --build
```
nginx раздаёт статику и проксирует `/api/` на `${BACKEND_URL}` (см. `nginx.conf`).

## Установка на iPhone
Safari → открыть адрес PWA → «Поделиться» → «На экран Домой».
Приложение запустится в standalone-режиме (полный экран).
> Для камеры (сканер, getUserMedia) iOS требует **HTTPS** (или `localhost`).
> В проде разместите PWA за TLS — иначе доступен ручной ввод штрихкода и выбор фото из галереи.

## Интеграция с API
Все эндпоинты из `docs/API.md` подключены полностью, без заглушек:
`auth/login|register|recover|refresh`, `ping`, `health`, `scan`,
`drafts/{id}/photos` (multipart + прогресс), `drafts/{id}/complete`,
`entries/{barcode}`, `photos/{storageKey}` (Bearer → object URL, кэш в SW).
