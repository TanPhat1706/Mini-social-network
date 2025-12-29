import React, { useState } from 'react';
import api from '../../api/api'; 
import { useAuth } from '../../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';

// Import CSS đã được "Facebook hóa"
import '../../assets/css/login.css'; 

const Login: React.FC = () => {
  // --- GIỮ NGUYÊN LOGIC CODE ---
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

  // --- GIAO DIỆN FACEBOOK STYLE ---
  return (
    <div className="fb-login-wrapper">
      <div className="fb-login-container">
        {/* CỘT TRÁI: THƯƠNG HIỆU */}
        <div className="fb-login-left">
          <h1 className="fb-logo">MiniSocial</h1>
          <p className="fb-tagline">
            MiniSocial giúp bạn kết nối và chia sẻ với mọi người trong cuộc sống của bạn.
          </p>
        </div>

        {/* CỘT PHẢI: FORM ĐĂNG NHẬP */}
        <div className="fb-login-right">
          <div className="fb-login-card">
            <form onSubmit={handleLogin}>
              <div className="form-group">
                <input 
                  className="fb-input"
                  type="text" 
                  placeholder="Email hoặc Mã Sinh Viên" 
                  value={identifier} 
                  onChange={(e) => setIdentifier(e.target.value)} 
                  required 
                />
              </div>
              <div className="form-group">
                <input 
                  className="fb-input"
                  type="password" 
                  placeholder="Mật khẩu" 
                  value={password} 
                  onChange={(e) => setPassword(e.target.value)} 
                  required 
                />
              </div>
              <button type="submit" className="fb-btn-login" disabled={loading}>
                {loading ? "Đang xử lý..." : "Đăng nhập"}
              </button>
            </form>

            <div className="fb-forgot-password">
              <Link to="/forgot-password">Quên mật khẩu?</Link>
            </div>

            <div className="fb-divider"></div>

            <div className="fb-create-account">
              <Link to="/register" className="fb-btn-register">
                Tạo tài khoản mới
              </Link>
            </div>
          </div>
          <div className="fb-login-footer">
            <b>Tạo Trang</b> dành cho người nổi tiếng, thương hiệu hoặc doanh nghiệp.
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;