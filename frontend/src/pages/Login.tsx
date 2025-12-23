import React, { useState } from 'react';
import axiosClient from '../api/axiosClient';
import { useAuth } from '../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import './Login.css';

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
      navigate('/'); 
    } catch (error: any) {
      alert(error.response?.data || "Đăng nhập thất bại");
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-content">
        <div className="auth-intro">
          <div className="auth-logo">MiniSocial</div>
          <div className="auth-desc">Kết nối với bạn bè và thế giới xung quanh bạn trên MiniSocial.</div>
        </div>
        
        <div className="auth-form-box">
          <form onSubmit={handleLogin}>
            <input 
              className="auth-input" 
              placeholder="Email hoặc Mã sinh viên" 
              value={identifier}
              onChange={e => setIdentifier(e.target.value)}
              required
            />
            <input 
              className="auth-input" 
              type="password" 
              placeholder="Mật khẩu"
              value={password}
              onChange={e => setPassword(e.target.value)}
              required
            />
            <button type="submit" className="btn-primary">Đăng nhập</button>
          </form>
          
          <div style={{marginTop: '15px'}}>
             <span className="auth-link">Quên mật khẩu?</span>
          </div>
          
          <div className="divider"></div>
          
          <Link to="/register">
            <button className="btn-success">Tạo tài khoản mới</button>
          </Link>
        </div>
      </div>
    </div>
  );
};
export default Login;