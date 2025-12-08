// src/components/auth/LoginForm.tsx
import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext'; 

const LoginForm: React.FC = () => {
    const [studentCode, setStudentCode] = useState<string>('');
    const [password, setPassword] = useState<string>('');
    const [error, setError] = useState<string | null>(null);
    const { login } = useAuth(); 

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        try {
            await login(studentCode, password); 
        } catch (err) {
            // TypeScript: err có kiểu unknown
            
            // Xử lý thông báo lỗi tùy thuộc vào kiểu lỗi trả về (ví dụ: lỗi Axios)
            let errorMessage = "Đăng nhập thất bại. Vui lòng kiểm tra lại Mã sinh viên/Email và Mật khẩu.";
            
            if (err instanceof Error) {
                errorMessage = err.message;
            }

            setError(errorMessage);
        }
    };

    return (
        <form onSubmit={handleSubmit} className="login-form">
            <input
                type="text"
                placeholder="Mã sinh viên hoặc Email"
                value={studentCode}
                onChange={(e) => setStudentCode(e.target.value)}
            />
            <input
                type="password"
                placeholder="Mật khẩu"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
            />
            <button
                type="submit"
                className="btn-login"
            >
                Đăng nhập
            </button>
            {error && <p style={{ color: 'red', textAlign: 'center' }}>{error}</p>}
            <a href="#" className="forgot-password-link">
                Quên mật khẩu?
            </a>
        </form>
    );
};

export default LoginForm;