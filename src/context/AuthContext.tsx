import { createContext, useState, useContext, useEffect } from 'react';
import type { ReactNode } from 'react';

// 1. Định nghĩa kiểu dữ liệu User cho chuẩn chỉnh
export interface UserInfo {
  id: number;
  studentCode: string;
  fullName: string;
  role: string;
  avatarUrl?: string;
}

// 2. Cập nhật Interface Context
interface AuthContextType {
  isAuthenticated: boolean;
  user: UserInfo | null; // Thêm biến user
  login: (token: string, userInfo: UserInfo) => void; // Hàm login nhận thêm userInfo
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider = ({ children }: AuthProviderProps) => {
  // State xác thực
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(() => {
    return !!localStorage.getItem('token');
  });

  // ⭐️ State lưu thông tin User (Khởi tạo từ LocalStorage nếu có)
  const [user, setUser] = useState<UserInfo | null>(() => {
    const savedUser = localStorage.getItem('user');
    return savedUser ? JSON.parse(savedUser) : null;
  });

  // ⭐️ Hàm Login mới: Lưu cả Token và User
  const login = (token: string, userInfo: UserInfo) => {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(userInfo)); // Lưu object dạng chuỗi
    
    setIsAuthenticated(true);
    setUser(userInfo);
  };

  // ⭐️ Hàm Logout: Xóa sạch dấu vết
  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    
    setIsAuthenticated(false);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ isAuthenticated, user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

// Hook để dùng Context
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};