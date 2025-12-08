// src/services/auth.service.ts
import axios from 'axios';
import type { IJwtResponse, IRegisterData } from '../types/auth'; // <-- Sử dụng import type cho cả hai

const API_URL = 'http://localhost:8080/api/auth/';

// Hàm đăng ký
const register = (data: IRegisterData) => {
  // Trả về promise từ axios.post. Nếu thành công, nó trả về 200/201.
  return axios.post(API_URL + 'register', data);
};

// Hàm đăng nhập
const login = (studentCode: string, password: string): Promise<IJwtResponse> => {
  return axios
    .post(API_URL + 'login', { studentCode, password })
    .then((response) => {
      const data: IJwtResponse = response.data;
      if (data.accessToken) {
        // Lưu response vào Local Storage khi đăng nhập thành công
        localStorage.setItem('user', JSON.stringify(data));
      }
      return data;
    });
};

// Hàm đăng xuất
const logout = () => {
    // Nếu bạn có API logout (nên có), gọi nó ở đây
    // axios.post(API_URL + 'logout'); 
    localStorage.removeItem('user');
};

export default {
  register,
  login,
  logout,
};