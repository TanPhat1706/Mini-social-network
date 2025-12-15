import React, { useState } from 'react';
import axiosClient from '../api/axiosClient';
import { useNavigate, Link } from 'react-router-dom';

const Register: React.FC = () => {
  const [formData, setFormData] = useState({
    studentCode: '', fullName: '', email: '', password: '', className: ''
  });
  const navigate = useNavigate();

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await axiosClient.post('/register', formData);
      alert("Đăng ký thành công!");
      navigate('/login');
    } catch (error: any) {
      alert(error.response?.data || "Lỗi đăng ký");
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({...formData, [e.target.name]: e.target.value});
  }

  return (
    <div className="auth-wrapper">
      <div className="auth-card">
        <h2 className="auth-title">Tạo tài khoản</h2>
        <p className="auth-subtitle">Tham gia cộng đồng sinh viên ngay hôm nay</p>

        <form onSubmit={handleRegister}>
          <div className="form-group">
            <input name="fullName" className="form-input" placeholder="Họ và tên đầy đủ" onChange={handleChange} required />
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '15px' }}>
             <div className="form-group">
                <input name="studentCode" className="form-input" placeholder="Mã SV" onChange={handleChange} required />
             </div>
             <div className="form-group">
                <input name="className" className="form-input" placeholder="Lớp" onChange={handleChange} />
             </div>
          </div>
          <div className="form-group">
            <input name="email" type="email" className="form-input" placeholder="Địa chỉ Email" onChange={handleChange} required />
          </div>
          <div className="form-group">
            <input name="password" type="password" className="form-input" placeholder="Mật khẩu" onChange={handleChange} required />
          </div>
          
          <button type="submit" className="btn-primary">Đăng ký tài khoản</button>
        </form>

        <div className="auth-footer">
          Đã có tài khoản? <Link to="/login" className="auth-link">Đăng nhập</Link>
        </div>
      </div>
    </div>
  );
};

export default Register;