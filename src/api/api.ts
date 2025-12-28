import axios from 'axios';
import type {
  AxiosInstance,
  InternalAxiosRequestConfig,
  AxiosResponse,
} from 'axios';

// 1. Tạo instance của Axios với cấu hình cơ bản
const api: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'https://mini-social-network-ayab.onrender.com',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
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
