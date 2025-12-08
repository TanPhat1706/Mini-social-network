// src/App.tsx
import React from 'react';
// Import các thành phần định tuyến
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
// Import các trang và Context
import AuthPage from './pages/AuthPage';
import HomePage from './pages/HomePage'; 
import { AuthProvider, useAuth } from './context/AuthContext';

// --- 1. ĐỊNH NGHĨA PROPS CHO PROTECTED ROUTE ---
/**
 * Định nghĩa kiểu dữ liệu cho props của ProtectedRoute.
 * Chỉ nhận children (các component được bọc bên trong).
 */
interface ProtectedRouteProps {
    children: React.ReactNode;
}

// --- 2. COMPONENT ROUTE BẢO VỆ (PROTECTED ROUTE) ---
/**
 * Component kiểm tra trạng thái đăng nhập của người dùng.
 * Nếu không có currentUser, chuyển hướng về trang /login.
 */
const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
  // Lấy trạng thái đăng nhập từ AuthContext
  const { currentUser } = useAuth();
  
  if (!currentUser) {
    // Nếu chưa đăng nhập, chuyển hướng về /login và thay thế lịch sử trình duyệt
    return <Navigate to="/login" replace />;
  }
  // Nếu đã đăng nhập, hiển thị component con (children)
  return <>{children}</>;
};

// --- 3. COMPONENT CHÍNH APP ---
const App: React.FC = () => {
  return (
    <Router>
      {/* AuthProvider bao bọc toàn bộ ứng dụng để chia sẻ trạng thái xác thực */}
      <AuthProvider>
        <Routes>
          
          {/* 1. Public Routes: Các trang công khai (Login, Register) */}
          <Route path="/login" element={<AuthPage />} />
          <Route path="/register" element={<AuthPage />} />
          
          {/* 2. Protected Route: Trang cần xác thực (HomePage) */}
          <Route 
            path="/home" 
            element={
              <ProtectedRoute>
                <HomePage />
              </ProtectedRoute>
            } 
          />
          
          {/* 3. Catch-all Route: Xử lý các đường dẫn không khớp, chuyển hướng về /login */}
          <Route path="*" element={<Navigate to="/login" replace />} />
          
        </Routes>
      </AuthProvider>
    </Router>
  );
}

export default App;