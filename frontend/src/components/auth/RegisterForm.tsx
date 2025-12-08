// src/components/auth/RegisterForm.tsx
import React, { useState } from 'react';
import type { IRegisterData } from '../../types/auth';
import authService from '../../services/auth.service';

interface RegisterFormProps {
    onSuccess: () => void; 
    onSwitchToLogin: () => void; 
}

const RegisterForm: React.FC<RegisterFormProps> = ({ onSuccess, onSwitchToLogin }) => {
    const [formData, setFormData] = useState<IRegisterData>({
        studentCode: '',
        email: '',
        password: '',
        fullName: '',
        className: '',
    });
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState<boolean>(false);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        setLoading(true);

        try {
            await authService.register(formData);
            onSuccess(); 
        } catch (err) {
            let errorMessage = "Đăng ký thất bại. Vui lòng thử lại.";
            
            // Xử lý lỗi từ Axios/Backend
            if (err && typeof err === 'object' && 'response' in err && err.response && 'data' in err.response && err.response.data && 'message' in err.response.data) {
                errorMessage = err.response.data.message;
            } else if (err instanceof Error) {
                 errorMessage = "Lỗi kết nối. Vui lòng kiểm tra server.";
            }
            
            setError(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    return (
        <form onSubmit={handleSubmit} className="register-form">
            <h2 className="register-header">Tạo tài khoản mới</h2>
            <p className="register-subheader">Nhanh chóng và dễ dàng.</p>
            
            {/* INPUT GROUP 2 CỘT */}
            <div className="register-input-group">
                <input type="text" name="fullName" placeholder="Tên đầy đủ" value={formData.fullName} onChange={handleChange} required />
                <input type="text" name="studentCode" placeholder="Mã sinh viên" value={formData.studentCode} onChange={handleChange} required />
            </div>

            {/* INPUT 1 CỘT */}
            <input type="email" name="email" placeholder="Email" value={formData.email} onChange={handleChange} required />
            <input type="text" name="className" placeholder="Tên lớp (Ví dụ: KTPM2023)" value={formData.className} onChange={handleChange} />
            <input type="password" name="password" placeholder="Mật khẩu mới" value={formData.password} onChange={handleChange} required />
            
            {/* HIỂN THỊ LỖI */}
            {error && <p style={{ color: 'red', textAlign: 'center', marginTop: '10px' }}>{error}</p>}
            
            {/* NÚT SUBMIT */}
            <div className="register-submit-area">
                <button
                    type="submit"
                    className="btn-register"
                    disabled={loading}
                >
                    {loading ? 'Đang đăng ký...' : 'Đăng ký'}
                </button>
            </div>
            
            {/* LINK CHUYỂN ĐỔI FORM */}
            <div style={{ textAlign: 'center', marginTop: '10px' }}>
                <a 
                    href="#" 
                    onClick={(e) => { e.preventDefault(); onSwitchToLogin(); }} 
                    className="form-switch-link"
                >
                    Đã có tài khoản? Đăng nhập.
                </a>
            </div>
        </form>
    );
};

export default RegisterForm;