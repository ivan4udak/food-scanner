import '@testing-library/jest-dom/vitest';
import { afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';

// Автоочистка DOM между тестами.
afterEach(() => {
  cleanup();
});
