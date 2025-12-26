import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ChatProvider } from './context/ChatContext'; // <--- 1. IMPORT CÁI NÀY
import Login from './pages/Login';
import Register from './pages/Register';
import Profile from './pages/Profile';
import Home from './pages/Home'; 
import type { JSX } from 'react';

// Component bảo vệ route
const PrivateRoute = ({ children }: { children: JSX.Element }) => {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? children : <Navigate to="/login" />;
};

function App() {
  return (
    <AuthProvider>
      {/* 2. BỌC CHAT PROVIDER Ở ĐÂY (Nên nằm trong AuthProvider để lấy được user info) */}
      <ChatProvider> 
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            
            <Route 
              path="/" 
              element={
                <PrivateRoute>
                  <Home />
                </PrivateRoute>
              } 
            />

            <Route 
              path="/profile" 
              element={
                <PrivateRoute>
                  <Profile />
                </PrivateRoute>
              } 
            />
            
            <Route path="*" element={<Navigate to="/" />} />
          </Routes>
        </BrowserRouter>
      </ChatProvider>
      {/* Kết thúc bọc ChatProvider */}
    </AuthProvider>
  );
}

export default App;