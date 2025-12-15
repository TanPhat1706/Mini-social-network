import React, { useState } from 'react';
import axiosClient from '../api/axiosClient';
import { useAuth } from '../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';

const Login: React.FC = () => {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await axiosClient.post('/login', { identifier, password });
      login(res.data.token);
      navigate('/profile');
    } catch (error: any) {
      alert(error.response?.data || "Đăng nhập thất bại");
    }
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-card">
        <h2 className="auth-title">Chào mừng trở lại</h2>
        <p className="auth-subtitle">Vui lòng đăng nhập tài khoản của bạn</p>
        
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
          <button type="submit" className="btn-primary">Đăng nhập ngay</button>
        </form>

        <div className="auth-footer">
          Chưa có tài khoản? <Link to="/register" className="auth-link">Đăng ký ngay</Link>
        </div>
      </div>
    </div>
  );
};

export default Login;