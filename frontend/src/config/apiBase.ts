/**
 * Gốc HTTP của backend (không có /api), ví dụ https://xxxx.ngrok-free.app
 * Dùng cho WebSocket (SockJS), ảnh /uploads, và axios gọi /api/... từ gốc.
 */
function trimEndSlash(s: string): string {
  return s.replace(/\/+$/, '');
}

export function getApiBaseUrl(): string {
  const explicit = import.meta.env.VITE_API_BASE_URL;
  if (explicit && String(explicit).trim()) {
    return trimEndSlash(String(explicit).trim());
  }
  const authUrl = import.meta.env.VITE_API_URL;
  if (authUrl && String(authUrl).includes('/api/auth')) {
    return trimEndSlash(String(authUrl).replace(/\/api\/auth\/?$/, ''));
  }
  return 'http://localhost:8080';
}

/**
 * Base cho axiosClient (đường dẫn kiểu /profile, /friends/...) — mặc định .../api/auth
 */
export function getApiAuthUrl(): string {
  const auth = import.meta.env.VITE_API_URL;
  if (auth && String(auth).trim()) {
    return trimEndSlash(String(auth).trim());
  }
  return `${getApiBaseUrl()}/api/auth`;
}
console.log('[API]', getApiBaseUrl(), getApiAuthUrl());
