import React, { useState } from 'react';
import api from '../../api/api';
import { useAuth } from '../../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import '../../assets/css/login.css';
import ForgotPasswordModal from '../../components/auth/ForgotPasswordModal';
import { showError } from '../../utils/swal';

const Login: React.FC = () => {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [forgotOpen, setForgotOpen] = useState(false);
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
      userInfo.role === 'ADMIN' ? navigate('/admin/dashboard') : navigate('/');
    } catch (error: any) {
      showError(error.response?.data || "Đăng nhập thất bại");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-wrapper">
      <div style={{ textAlign: 'center', maxWidth: '1000px', display: 'flex', alignItems: 'center', gap: '50px' }}>
        {/* Slogan kiểu Facebook */}
        <div style={{ textAlign: 'left', width: '500px' }} className="hidden-mobile">
          <h1 style={{ color: 'var(--fb-blue)', fontSize: '60px', marginBottom: '0' }}>Mini Social</h1>
          <p style={{ fontSize: '24px', lineHeight: '28px', color: '#1c1e21' }}>
            Hệ thống giúp bạn kết nối và chia sẻ với mọi người trong khoa.
          </p>
        </div>

        <div className="auth-card">
          <form onSubmit={handleLogin}>
            <input className="form-input" type="text" placeholder="Email hoặc Mã Sinh Viên" value={identifier} onChange={(e) => setIdentifier(e.target.value)} required />
            <input className="form-input" type="password" placeholder="Mật khẩu" value={password} onChange={(e) => setPassword(e.target.value)} required />
            <button type="submit" className="btn-login" disabled={loading}>
              {loading ? "Đang đăng nhập..." : "Đăng nhập"}
            </button>
          </form>
          <div style={{ textAlign: 'center', marginTop: '12px' }}>
            <button type="button" className="btn-register-new" style={{ backgroundColor: '#1877F2' }} onClick={() => setForgotOpen(true)}>
              Quên mật khẩu?
            </button>
          </div>
          <div style={{ borderBottom: '1px solid #dadde1', margin: '20px 0' }}></div>
          <button className="btn-register-new" onClick={() => navigate('/register')}>
            Tạo tài khoản mới
          </button>
        </div>
      </div>
      {forgotOpen && <ForgotPasswordModal onClose={() => setForgotOpen(false)} />}
    </div>
  );
};

export default Login;