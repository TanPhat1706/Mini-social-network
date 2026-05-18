import { Box } from '@mui/material';
import { Routes, Route, Navigate, Outlet } from 'react-router-dom';
import type { JSX } from 'react';

import Header from './components/layout/Header';
import Footer from './components/layout/Footer';
import { AdminLayout } from './components/layout/AdminLayout';

import HomePage from './pages/HomePage';
import ProfilePage from './pages/Profile';

import Login from './pages/auth/Login';
import Register from './pages/auth/Register';

import AdminDashboard from './pages/admin/AdminDashboard';
import { PostManager } from './pages/admin/PostManager';
import { UserManager } from './pages/admin/UserManager';

import { AuthProvider, useAuth } from './context/AuthContext';
import { WebSocketProvider } from './context/WebSocketContext';
import { ChatProvider } from './context/ChatContext';
import PostDetailPage from './pages/PostDetailPage';
import ShopPage from './pages/ShopPage';
import SecurityPage from './pages/settings/SecurityPage';

// 1️⃣ IMPORT COMPONENT GAME MỚI
import SnakeGame from './components/game/SnakeGame';
import TicTacToePage from './components/game/TicTacToePage'; // Đổi đường dẫn cho khớp với thư mục của bạn
import GameList from './pages/GameList';




/* ================= 🛡️ HỆ THỐNG LÍNH CANH ROUTE 🛡️ ================= */

// 1. LÍNH CANH KHÁCH (Dành cho Login/Register)
const GuestRoute = ({ children }: { children: JSX.Element }) => {
  const { isAuthenticated, user } = useAuth();
  if (isAuthenticated) {
    // Đã đăng nhập rồi thì không cho ở lại trang Login, phân luồng trả về đúng nhà!
    return user?.role === 'ADMIN' ? <Navigate to="/admin/dashboard" replace /> : <Navigate to="/" replace />;
  }
  return children;
};

// 2. LÍNH CANH NGƯỜI DÙNG THƯỜNG (Khóa Admin)
const UserRoute = ({ children }: { children: JSX.Element }) => {
  const { isAuthenticated, user } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;

  // 🚫 KHÓA CHẶT ADMIN: Nếu là ADMIN cố tình vào đây -> Đá về Dashboard
  if (user?.role === 'ADMIN') return <Navigate to="/admin/dashboard" replace />;

  return children;
};

// 3. LÍNH CANH QUẢN TRỊ VIÊN (Khóa User)
const AdminRoute = ({ children }: { children: JSX.Element }) => {
  const { isAuthenticated, user } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;

  // 🚫 KHÓA CHẶT USER: Nếu là USER cố tình gõ /admin -> Đá về Trang chủ
  if (user?.role !== 'ADMIN') return <Navigate to="/" replace />;

  return children;
};


/* ================= APP ================= */
function App() {
  return (
    <WebSocketProvider>
      <ChatProvider>
        <AuthProvider>
          <Box
            sx={{
              display: 'flex',
              flexDirection: 'column',
              minHeight: '100vh',
              bgcolor: 'background.default',
            }}
          >
            <Header />

            <main style={{ flexGrow: 1 }}>
              <Routes>
                {/* ===== PUBLIC / GUEST ROUTES ===== */}
                <Route path="/login" element={<GuestRoute><Login /></GuestRoute>} />
                <Route path="/register" element={<GuestRoute><Register /></GuestRoute>} />

                {/* ===== PROTECTED USER ROUTES (CẤM ADMIN) ===== */}
                <Route path="/" element={<UserRoute><HomePage /></UserRoute>} />
                <Route path="/profile" element={<UserRoute><ProfilePage /></UserRoute>} />
                <Route path="/profile/:userId" element={<UserRoute><ProfilePage /></UserRoute>} />
                <Route path="/posts/:postId" element={<UserRoute><PostDetailPage /></UserRoute>} />
                <Route path="/settings/security" element={
                  <UserRoute>
                    <Box sx={{ py: 4 }}><SecurityPage /></Box>
                  </UserRoute>
                } />
                <Route path="/games" element={
                  <UserRoute>
                    <Box sx={{ py: 4 }}><GameList /></Box>
                  </UserRoute>
                } />
                <Route path="/games/snake" element={
                  <UserRoute>
                    <Box sx={{ py: 4 }}><SnakeGame /></Box>
                  </UserRoute>
                } />
                <Route path="/games/tic-tac-toe/:sessionId" element={
                  <UserRoute>
                    <Box sx={{ py: 4 }}><TicTacToePage /></Box>
                  </UserRoute>
                } />
                <Route path="/shop" element={
                  <UserRoute>
                    <Box sx={{ py: 4 }}><ShopPage /></Box>
                  </UserRoute>
                } />

                {/* ===== ADMIN ROUTES (CẤM USER) ===== */}
                <Route path="/admin" element={
                  <AdminRoute>
                    <AdminLayout>
                      <Outlet />
                    </AdminLayout>
                  </AdminRoute>
                }>
                  <Route index element={<AdminDashboard />} />
                  <Route path="dashboard" element={<AdminDashboard />} />
                  <Route path="posts" element={<PostManager />} />
                  <Route path="users" element={<UserManager />} />
                  <Route path="*" element={<Navigate to="dashboard" replace />} />
                </Route>

                {/* ===== FALLBACK (404) ===== */}
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </main>

            <Footer />
          </Box>
        </AuthProvider>
      </ChatProvider>
    </WebSocketProvider>
  );
}

export default App;