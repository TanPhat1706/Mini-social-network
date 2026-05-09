import axios from 'axios';
import { getApiAuthUrl } from '../config/apiBase';

const baseURL = `${import.meta.env.VITE_API_BASE_URL}${import.meta.env.VITE_API_AUTH_ENDPOINT}`;

const axiosClient = axios.create({
  baseURL: getApiAuthUrl(),
  headers: {
    'Content-Type': 'application/json',
    'ngrok-skip-browser-warning': 'true',
  },
});

// Interceptor: Tự động gắn Token vào mọi request
axiosClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default axiosClient;
