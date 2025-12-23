import React, { useState } from 'react';
import axiosClient from '../api/axiosClient';
import { useNavigate, Link } from 'react-router-dom';
import './Register.css';

const Register: React.FC = () => {
  const navigate = useNavigate();
  
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    password: '',
    studentCode: '',
    className: ''
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      // Lưu ý: Nếu axiosClient của bạn có baseURL là .../api/auth 
      // thì giữ nguyên '/register'.
      // Nếu baseURL là .../api thì sửa thành '/auth/register'.
      await axiosClient.post('/register', formData);
      
      alert('Đăng ký thành công! Hãy đăng nhập ngay.');
      navigate('/login');
    } catch (error: any) {
      console.error(error);
      alert(error.response?.data?.message || 'Đăng ký thất bại. Vui lòng kiểm tra lại thông tin!');
    }
  };

  return (
    <div className="register-page">
      <div className="register-content">
        <div className="reg-brand">MiniSocial</div>
        <div className="reg-subtitle">Tạo tài khoản để kết nối với bạn bè.</div>

        <form onSubmit={handleRegister}>
          {/* Hàng 1: Họ tên + Mã SV */}
          <div className="reg-row">
            <div className="reg-form-group" style={{flex: 1}}>
              <input 
                name="fullName" 
                className="modern-input" 
                placeholder="Họ và tên" 
                required 
                onChange={handleChange}
              />
            </div>
            <div className="reg-form-group" style={{flex: 1}}>
              <input 
                name="studentCode" 
                className="modern-input" 
                placeholder="Mã SV" 
                required 
                onChange={handleChange}
              />
            </div>
          </div>

          <div className="reg-form-group">
            <input 
              name="email" 
              type="email"
              className="modern-input" 
              placeholder="Email hoặc số điện thoại" 
              required 
              onChange={handleChange}
            />
          </div>

          <div className="reg-form-group">
            <input 
              name="className" 
              className="modern-input" 
              placeholder="Lớp (Ví dụ: 12A1 - CNTT)" 
              required 
              onChange={handleChange}
            />
          </div>

          <div className="reg-form-group">
            <input 
              name="password" 
              type="password" 
              className="modern-input" 
              placeholder="Mật khẩu mới" 
              required 
              onChange={handleChange}
            />
          </div>

          <p style={{fontSize:'12px', color:'#777', textAlign:'left', marginBottom:'15px'}}>
            Bằng cách nhấp vào Đăng ký, bạn đồng ý với Điều khoản và Chính sách của MiniSocial.
          </p>

          <button type="submit" className="btn-submit-reg">Đăng ký</button>
        </form>
        
        <div className="reg-footer">
          Bạn đã có tài khoản? 
          <Link to="/login" className="link-login">Đăng nhập</Link>
        </div>
      </div>
    </div>
  );
};

export default Register;