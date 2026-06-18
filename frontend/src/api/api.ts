import axios from 'axios';
import type {
  AxiosInstance,
  InternalAxiosRequestConfig,
  AxiosResponse,
} from 'axios';
import { getApiBaseUrl } from '../config/apiBase';

/** Gốc backend: các request dùng đường dẫn đầy đủ /api/admin/..., /api/posts/... */
const api: AxiosInstance = axios.create({
  baseURL: getApiBaseUrl(),
  headers: {
    'Content-Type': 'application/json',
    'ngrok-skip-browser-warning': 'true',
  },
  timeout: Number(import.meta.env.VITE_API_TIMEOUT) || 10000,
});

// 2. REQUEST INTERCEPTOR
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token');

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// 3. RESPONSE INTERCEPTOR
api.interceptors.response.use(
  (response: AxiosResponse) => response,
  (error) => {
    if (error.response) {
      switch (error.response.status) {
        case 401:
          console.error('Lỗi 401: Phiên đăng nhập hết hạn.');
          break;
        case 403:
          console.error('Lỗi 403: Không có quyền truy cập.');
          break;
        case 500:
          console.error('Lỗi 500: Lỗi server.');
          break;
        default:
          console.error('Lỗi khác:', error.message);
      }
    }
    return Promise.reject(error);
  }
);

export default api;
