import React, { useState } from 'react';
import api from '../../api/api'; 
import { useAuth } from '../../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';

// Import CSS đã tối ưu
import '../../assets/css/login.css'; 

const Login: React.FC = () => {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await api.post('/api/auth/login', { identifier, password });
      
      const { token, ...userInfo } = res.data;
      login(token, userInfo); 
      
      // Chuyển hướng thông minh
      if (userInfo.role === 'ADMIN') {
          navigate('/admin/dashboard');
      } else {
          navigate('/');
      }
      
    } catch (error: any) {
      const msg = error.response?.data || "Đăng nhập thất bại";
      alert(typeof msg === 'string' ? msg : JSON.stringify(msg));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-card">
        <h2 className="auth-title">Chào mừng trở lại</h2>
        <p className="auth-subtitle">Đăng nhập để kết nối với hệ thống</p>
        
        <form onSubmit={handleLogin}>
          <div className="form-group">
            <input 
              className="form-input"
              type="text" 
              placeholder="Email hoặc Mã Sinh Viên" 
              value={identifier} 
              onChange={(e) => setIdentifier(e.target.value)} 
              required 
            />
          </div>
          <div className="form-group">
            <input 
              className="form-input"
              type="password" 
              placeholder="Mật khẩu" 
              value={password} 
              onChange={(e) => setPassword(e.target.value)} 
              required 
            />
          </div>
          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? "Đang xử lý..." : "Đăng nhập ngay"}
          </button>
        </form>

        <div className="auth-footer">
          Chưa có tài khoản? <Link to="/register" className="auth-link">Đăng ký ngay</Link>
        </div>
      </div>
    </div>
  );
};

export default Login;