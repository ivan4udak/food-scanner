/// <reference types="vitest" />
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';
import { fileURLToPath, URL } from 'node:url';

// Backend для dev-прокси (в проде проксирует nginx).
// Переопределяется через VITE_API_TARGET (см. .env.example).
const API_TARGET = process.env.VITE_API_TARGET ?? 'http://localhost:8080';

export default defineConfig({
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },

  server: {
    port: 5173,
    proxy: {
      // Один origin для фронта и API → нет CORS, работает Bearer и install-критерии PWA.
      '/api': {
        target: API_TARGET,
        changeOrigin: true,
      },
    },
  },

  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['icon.svg', 'apple-touch-icon.png', 'robots.txt'],
      manifest: {
        name: 'Food Scanner',
        short_name: 'FoodScanner',
        description: 'Сканирование штрихкодов и каталогизация продуктов.',
        lang: 'ru',
        theme_color: '#0B0B0F',
        background_color: '#0B0B0F',
        display: 'standalone',
        orientation: 'portrait',
        start_url: '/',
        scope: '/',
        icons: [
          { src: 'pwa-192.png', sizes: '192x192', type: 'image/png' },
          { src: 'pwa-512.png', sizes: '512x512', type: 'image/png' },
          { src: 'pwa-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
        ],
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,svg,png,ico,woff2}'],
        navigateFallback: '/index.html',
        // Никогда не отдавать из кэша запросы к API навигацией.
        navigateFallbackDenylist: [/^\/api\//],
        runtimeCaching: [
          {
            // Офлайн-просмотр каталога (как офлайн-кэш iOS, Блок 18).
            urlPattern: ({ url }) => url.pathname.startsWith('/api/v1/entries/'),
            handler: 'NetworkFirst',
            options: {
              cacheName: 'api-entries',
              networkTimeoutSeconds: 4,
              expiration: { maxEntries: 200, maxAgeSeconds: 60 * 60 * 24 * 30 },
              cacheableResponse: { statuses: [0, 200] },
            },
          },
          {
            // Кэш изображений каталога (как дисковый кэш iOS, Блок 9).
            urlPattern: ({ url }) => url.pathname.startsWith('/api/v1/photos/'),
            handler: 'CacheFirst',
            options: {
              cacheName: 'api-photos',
              expiration: { maxEntries: 500, maxAgeSeconds: 60 * 60 * 24 * 30 },
              cacheableResponse: { statuses: [0, 200] },
            },
          },
        ],
      },
      devOptions: { enabled: false },
    }),
  ],

  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
  },
});
