/**
 * Gốc HTTP của backend (không có /api), ví dụ https://xxxx.ngrok-free.app
 * Dùng cho WebSocket (SockJS), ảnh /uploads, và axios gọi /api/... từ gốc.
 */
function trimEndSlash(s: string): string {
  let result = s;
  // Dùng vòng lặp cắt chuỗi thay vì Regex để tránh rủi ro ReDoS
  while (result.endsWith('/')) {
    result = result.slice(0, -1);
  }
  return result;
}

export function getApiBaseUrl(): string {
  const explicit = import.meta.env.VITE_API_BASE_URL;
  if (explicit && String(explicit).trim()) {
    return trimEndSlash(String(explicit).trim());
  }

  const authUrl = import.meta.env.VITE_API_URL;
  if (authUrl && String(authUrl).includes('/api/auth')) {
    // Loại bỏ Regex tiềm ẩn rủi ro ở đây luôn cho đồng bộ
    let base = String(authUrl).trim();
    if (base.endsWith('/')) base = base.slice(0, -1);
    if (base.endsWith('/api/auth')) base = base.slice(0, -'/api/auth'.length);
    return trimEndSlash(base);
  }

  return 'http://localhost:8080';
}

function ensureLeadingSlash(path: string): string {
  return path.startsWith('/') ? path : `/${path}`;
}

/**
 * Base cho axiosClient (đường dẫn kiểu /profile, /friends/...) — mặc định .../api/auth
 */
export function getApiAuthUrl(): string {
  const auth = import.meta.env.VITE_API_URL;
  if (auth && String(auth).trim()) {
    return trimEndSlash(String(auth).trim());
  }

  const baseUrl = getApiBaseUrl();
  const authEndpoint = import.meta.env.VITE_API_AUTH_ENDPOINT;
  if (authEndpoint && String(authEndpoint).trim()) {
    return `${trimEndSlash(baseUrl)}${ensureLeadingSlash(String(authEndpoint).trim())}`;
  }

  return `${baseUrl}/api/auth`;
}

console.log('[API]', getApiBaseUrl(), getApiAuthUrl());