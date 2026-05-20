import React, { useState } from 'react';
import api from '../../api/api';
import { useNavigate } from 'react-router-dom';
import '../../assets/css/login.css';
import { showSuccess, showError } from '../../utils/swal';

const Register: React.FC = () => {
  const [role, setRole] = useState<'STUDENT' | 'TEACHER'>('STUDENT');
  const [formData, setFormData] = useState({ studentCode: '', fullName: '', email: '', password: '', className: '' });
  const navigate = useNavigate();

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.post('/api/auth/register', { ...formData, role });
      showSuccess("Đăng ký thành công! Chờ Admin duyệt.");
      navigate('/login');
    } catch (error: any) {
      showError(error.response?.data || "Lỗi đăng ký");
    }
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-card" style={{ maxWidth: '432px' }}>
        <h2 style={{ fontSize: '32px', marginBottom: '8px' }}>Đăng ký</h2>
        <p style={{ color: '#606770', marginBottom: '20px' }}>Nhanh chóng và dễ dàng.</p>
        <div style={{ borderBottom: '1px solid #dadde1', marginBottom: '15px' }}></div>
        
        <form onSubmit={handleRegister}>
          <input name="fullName" className="form-input" placeholder="Họ tên" onChange={(e) => setFormData({...formData, fullName: e.target.value})} required />
          
          <div style={{ display: 'flex', gap: '10px', marginBottom: '12px' }}>
            <label style={{ flex: 1, border: '1px solid #dddfe2', padding: '10px', borderRadius: '6px', display: 'flex', justifyContent: 'space-between' }}>
              Sinh viên <input type="radio" name="role" checked={role === 'STUDENT'} onChange={() => setRole('STUDENT')} />
            </label>
            <label style={{ flex: 1, border: '1px solid #dddfe2', padding: '10px', borderRadius: '6px', display: 'flex', justifyContent: 'space-between' }}>
              Giảng viên <input type="radio" name="role" checked={role === 'TEACHER'} onChange={() => setRole('TEACHER')} />
            </label>
          </div>

          <input name="studentCode" className="form-input" placeholder={role === 'STUDENT' ? "Mã Sinh viên" : "Mã Giảng viên"} onChange={(e) => setFormData({...formData, studentCode: e.target.value})} required />
          {role === 'STUDENT' && <input name="className" className="form-input" placeholder="Lớp (VD: KTPM01)" onChange={(e) => setFormData({...formData, className: e.target.value})} />}
          <input name="email" className="form-input" placeholder="Email" onChange={(e) => setFormData({...formData, email: e.target.value})} required />
          <input name="password" type="password" className="form-input" placeholder="Mật khẩu mới" onChange={(e) => setFormData({...formData, password: e.target.value})} required />
          
          <p style={{ fontSize: '11px', color: '#777', margin: '15px 0' }}>Bằng cách nhấp vào Đăng ký, bạn đồng ý với Điều khoản của khoa.</p>
          <button type="submit" className="btn-register-new" style={{ width: '200px', display: 'block', margin: '0 auto' }}>Đăng ký</button>
        </form>
      </div>
    </div>
  );
};

export default Register;