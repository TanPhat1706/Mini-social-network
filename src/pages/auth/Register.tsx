import React, { useState } from 'react';
import api from '../../api/api';
import { useNavigate, Link } from 'react-router-dom';
import '../../assets/css/regis.css';

const Register: React.FC = () => {
  const [role, setRole] = useState<'STUDENT' | 'TEACHER'>('STUDENT');
  const [formData, setFormData] = useState({
    studentCode: '', fullName: '', email: '', password: '', className: ''
  });
  
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const payload = { ...formData, role };
      await api.post('/api/auth/register', payload);
      alert("Đăng ký thành công! Vui lòng chờ Admin duyệt.");
      navigate('/login');
    } catch (error: any) {
      alert(error.response?.data || "Lỗi đăng ký");
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({...formData, [e.target.name]: e.target.value});
  }

  return (
    <div className="auth-wrapper">
      <div className="auth-card">
        <h2 className="auth-title" style={{ color: '#1c1e21', fontSize: '32px' }}>Đăng ký</h2>
        <p className="auth-subtitle" style={{ borderBottom: '1px solid #dddfe2', paddingBottom: '15px' }}>
          Nhanh chóng và dễ dàng.
        </p>
        
        <div className="role-btn-group" style={{ marginTop: '15px' }}>
          <button type="button" className={`btn-role ${role === 'STUDENT' ? 'active' : ''}`} onClick={() => setRole('STUDENT')}>Sinh viên</button>
          <button type="button" className={`btn-role ${role === 'TEACHER' ? 'active' : ''}`} onClick={() => setRole('TEACHER')}>Giảng viên</button>
        </div>

        <form onSubmit={handleRegister}>
          <div className="form-group">
            <input name="fullName" className="form-input" placeholder="Họ và tên" onChange={handleChange} required />
          </div>
          
          <div style={{ display: 'flex', gap: '10px' }}>
             <input name="studentCode" className="form-input" placeholder={role === 'STUDENT' ? "Mã SV" : "Mã GV"} onChange={handleChange} required />
             {role === 'STUDENT' && (
                <input name="className" className="form-input" placeholder="Lớp" onChange={handleChange} />
             )}
          </div>

          <div className="form-group" style={{ marginTop: '15px' }}>
            <input name="email" type="email" className="form-input" placeholder="Email" onChange={handleChange} required />
          </div>
          
          <div className="form-group">
            <input name="password" type="password" className="form-input" placeholder="Mật khẩu mới" onChange={handleChange} required />
          </div>
          
          <button type="submit" className="btn-primary" disabled={loading} style={{ background: '#00a400', marginTop: '10px' }}>
            {loading ? "Đang tạo..." : "Đăng ký"}
          </button>
        </form>

        <div className="auth-footer">
          <Link to="/login" className="auth-link">Bạn đã có tài khoản?</Link>
        </div>
      </div>
    </div>
  );
};

export default Register;