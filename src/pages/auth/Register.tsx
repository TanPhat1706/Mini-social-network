import React, { useState } from 'react';
import api from '../../api/api';
import { useNavigate, Link } from 'react-router-dom';

// Import CSS đã tối ưu
import '../../assets/css/regis.css';

const Register: React.FC = () => {
  // State quản lý Role đang chọn (Mặc định là STUDENT)
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
      // Gửi role kèm theo data form
      const payload = {
        ...formData,
        role: role // ⭐️ Gửi role về backend
      };

      await api.post('/api/auth/register', payload);
      alert("Đăng ký thành công! Vui lòng chờ Admin duyệt.");
      navigate('/login');
    } catch (error: any) {
      const msg = error.response?.data || "Lỗi đăng ký";
      alert(typeof msg === 'string' ? msg : JSON.stringify(msg));
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
        <h2 className="auth-title">Tạo tài khoản mới</h2>
        <p className="auth-subtitle">Bạn tham gia với tư cách là?</p>
        
        {/* ⭐️ ROLE SWITCHER UI */}
        <div style={{ display: 'flex', gap: '10px', marginBottom: '25px' }}>
            <button 
                type="button"
                className="btn-primary"
                onClick={() => setRole('STUDENT')}
                style={{ 
                    background: role === 'STUDENT' ? undefined : '#E5E7EB',
                    color: role === 'STUDENT' ? 'white' : '#374151',
                    boxShadow: 'none', padding: '10px'
                }}
            >
                Học sinh
            </button>
            <button 
                type="button"
                className="btn-primary"
                onClick={() => setRole('TEACHER')}
                style={{ 
                    background: role === 'TEACHER' ? undefined : '#E5E7EB',
                    color: role === 'TEACHER' ? 'white' : '#374151',
                    boxShadow: 'none', padding: '10px'
                }}
            >
                Giảng viên
            </button>
        </div>

        <form onSubmit={handleRegister}>
          <div className="form-group">
            <input name="fullName" className="form-input" placeholder="Họ và tên đầy đủ" onChange={handleChange} required />
          </div>
          
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '15px' }}>
             <div className="form-group">
                {/* ⭐️ Thay đổi Placeholder theo Role */}
                <input 
                    name="studentCode" 
                    className="form-input" 
                    placeholder={role === 'STUDENT' ? "Mã Sinh viên" : "Mã Giảng viên"} 
                    onChange={handleChange} 
                    required 
                />
             </div>
             
             {/* ⭐️ Chỉ hiện ô nhập Lớp nếu là STUDENT */}
             {role === 'STUDENT' && (
                 <div className="form-group">
                    <input name="className" className="form-input" placeholder="Lớp (VD: KTPM01)" onChange={handleChange} />
                 </div>
             )}
          </div>

          <div className="form-group">
            <input name="email" type="email" className="form-input" placeholder="Địa chỉ Email" onChange={handleChange} required />
          </div>
          
          <div className="form-group">
            <input name="password" type="password" className="form-input" placeholder="Mật khẩu" onChange={handleChange} required />
          </div>
          
          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? "Đang xử lý..." : `Đăng ký với vai trò là ${role === 'STUDENT' ? 'Sinh viên' : 'Giảng viên'}`}
          </button>
        </form>

        <div className="auth-footer">
          Đã có tài khoản? <Link to="/login" className="auth-link">Đăng nhập ngay</Link>
        </div>
      </div>
    </div>
  );
};

export default Register;