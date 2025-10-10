// Centralized API base URL helper
// Reads from VITE_API_BASE_URL; falls back to http://localhost:8080 for local dev.

export const API_BASE: string = (import.meta.env?.VITE_API_BASE_URL as string) || 'http://localhost:8080';

export const apiUrl = (path: string) => {
  if (!path.startsWith('/')) return `${API_BASE}/${path}`;
  return `${API_BASE}${path}`;
};

