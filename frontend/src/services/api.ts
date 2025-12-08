// src/services/api.ts
import axios, { 
    type AxiosResponse, 
    type InternalAxiosRequestConfig 
} from 'axios';
import type { IJwtResponse, ITokenRefreshResponse } from '../types/auth'; // Đảm bảo dùng 'type'

const API_URL = 'http://localhost:8080/api/';

const instance = axios.create({
    baseURL: API_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

let isRefreshing = false;
let failedQueue: Array<{ resolve: (token: string) => void; reject: (error: Error) => void; }> = [];

const processQueue = (error: Error | null, token: string | null = null) => {
    failedQueue.forEach(prom => {
        if (error) {
            prom.reject(error);
        } else if (token) {
            prom.resolve(token);
        }
    });
    failedQueue = [];
};

// --- INTERCEPTOR REQUEST: Gắn Access Token ---
instance.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        const userJson = localStorage.getItem('user');
        const user: IJwtResponse | null = userJson ? JSON.parse(userJson) : null;
        const token = user?.accessToken;

        // Chỉ gắn Header nếu token tồn tại
        if (token) {
            config.headers.set('Authorization', 'Bearer ' + token);
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// --- INTERCEPTOR RESPONSE: Xử lý Refresh Token (401) ---
instance.interceptors.response.use(
    (response) => response,
    async (error) => {
        if (!error.response) {
            return Promise.reject(error);
        }

        const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }; 
        
        // Điều kiện Refresh Token: Lỗi 401 VÀ chưa thử lại
        if (error.response.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            // Xếp hàng nếu đang có quá trình Refresh Token đang diễn ra
            if (isRefreshing) {
                return new Promise<string>(function(resolve, reject) {
                    failedQueue.push({ resolve, reject: (e) => reject(e as Error) }); 
                }).then(token => {
                    originalRequest.headers.set('Authorization', 'Bearer ' + token);
                    return instance(originalRequest);
                }).catch(err => {
                    return Promise.reject(err);
                });
            }

            isRefreshing = true;

            const userJson = localStorage.getItem('user');
            const user: IJwtResponse | null = userJson ? JSON.parse(userJson) : null;
            const refreshToken = user?.refreshToken;

            if (refreshToken) {
                try {
                    // Gọi API Refresh Token bằng axios gốc
                    const rs: AxiosResponse<ITokenRefreshResponse> = await axios.post(
                        API_URL + 'auth/refresh-token', 
                        { refreshToken }
                    );

                    const { accessToken: newAccessToken } = rs.data;

                    // Cập nhật Local Storage
                    const updatedUser: IJwtResponse = { ...user!, accessToken: newAccessToken };
                    localStorage.setItem('user', JSON.stringify(updatedUser));

                    // Xử lý hàng chờ và thử lại request ban đầu
                    processQueue(null, newAccessToken);
                    
                    originalRequest.headers.set('Authorization', 'Bearer ' + newAccessToken);
                    return instance(originalRequest);

                } catch (err) {
                    // Refresh thất bại, buộc đăng xuất
                    const refreshError = err instanceof Error ? err : new Error("Token refresh failed");
                    processQueue(refreshError, null);
                    localStorage.removeItem('user');
                    window.location.href = '/login'; 
                    return Promise.reject(err);
                } finally {
                    isRefreshing = false;
                }
            } else {
                // Không có Refresh Token, buộc đăng xuất
                localStorage.removeItem('user');
                window.location.href = '/login'; 
                return Promise.reject(error);
            }
        }
        return Promise.reject(error);
    }
);

export default instance;