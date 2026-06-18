import { createContext, useState, useContext, useEffect } from 'react';
import type { ReactNode } from 'react';

// 1. Định nghĩa kiểu dữ liệu User cho chuẩn chỉnh
export interface UserInfo {
  id: number;
  studentCode: string;
  fullName: string;
  role: string;
  email?: string;
  className?: string;
  bio?: string;
  avatarUrl?: string;
  coverPhotoUrl?: string;
  active?: boolean;
  createdAt?: string;
  lastLogin?: string;
  level?: number;
  exp?: number;
  vptlPoints?: number;
  currentAvatarFrame?: string | null;
  currentNameColor?: string | null;
}

// 2. Cập nhật Interface Context
interface AuthContextType {
  isAuthenticated: boolean;
  user: UserInfo | null;
  login: (token: string, userInfo: UserInfo) => void;
  logout: () => void;
  // 🎯 THÊM MỚI: Hàm để cập nhật thông tin user (dùng Partial để chỉ cần truyền trường muốn đổi)
  updateUser: (updatedInfo: Partial<UserInfo>) => void; 
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

  // 🎯 THÊM MỚI: Logic cập nhật thông tin người dùng đang đăng nhập
  const updateUser = (updatedInfo: Partial<UserInfo>) => {
    setUser((prevUser) => {
      if (!prevUser) return null;
      
      // Trộn thông tin cũ và thông tin mới lại với nhau
      const newUser = { ...prevUser, ...updatedInfo };
      
      // Lưu đè lại vào LocalStorage để F5 không bị mất
      localStorage.setItem('user', JSON.stringify(newUser));
      
      return newUser;
    });
  };

  return (
    // 🎯 THÊM updateUser vào Provider
    <AuthContext.Provider value={{ isAuthenticated, user, login, logout, updateUser }}>
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
