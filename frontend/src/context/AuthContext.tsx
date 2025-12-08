// src/context/AuthContext.tsx
import React, { createContext, useContext, useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../services/auth.service';
import type { IAuthContext, IJwtResponse } from '../types/auth'; // Import types

// Khởi tạo context với giá trị mặc định là null (hoặc throw error nếu truy cập ngoài Provider)
const AuthContext = createContext<IAuthContext | null>(null);

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
    children: React.ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [currentUser, setCurrentUser] = useState<IJwtResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const navigate = useNavigate();

  useEffect(() => {
    const userJson = localStorage.getItem('user');
    if (userJson) {
        try {
            const user: IJwtResponse = JSON.parse(userJson);
            setCurrentUser(user);
        } catch (e) {
            console.error("Lỗi phân tích JSON từ Local Storage", e);
            localStorage.removeItem('user');
        }
    }
    setLoading(false);
  }, []);

  const login = async (studentCode: string, password: string) => {
    const data = await authService.login(studentCode, password);
    setCurrentUser(data);
    navigate('/home');
    return data;
  };

  const logout = () => {
    authService.logout();
    setCurrentUser(null);
    navigate('/login');
  };

  const value = useMemo(() => ({
    currentUser,
    login,
    logout,
    accessToken: currentUser ? currentUser.accessToken : null,
  }), [currentUser]);

  if (loading) return <div>Loading...</div>;

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};