// src/pages/AuthPage.tsx
import React, { useState } from 'react';
import LoginForm from '../components/auth/LoginForm';
import RegisterForm from '../components/auth/RegisterForm'; // Import component mới

const AuthPage: React.FC = () => {
    // State để quản lý form đang hiển thị
    const [isRegisterMode, setIsRegisterMode] = useState<boolean>(false);
    const [registrationSuccess, setRegistrationSuccess] = useState<boolean>(false);

    const handleRegistrationSuccess = () => {
        setRegistrationSuccess(true);
        setIsRegisterMode(false); // Chuyển về chế độ Login sau khi thành công
    };

    const handleSwitchToRegister = () => {
        setRegistrationSuccess(false);
        setIsRegisterMode(true);
    };

    const handleSwitchToLogin = () => {
        setRegistrationSuccess(false);
        setIsRegisterMode(false);
    };

    return (
        <div className="auth-page-container">
            <div className="auth-content-wrapper">
                
                {/* Phần Giới Thiệu (Left) */}
                <div className="auth-intro">
                    <h1>Mini Social</h1>
                    <p>Kết nối với bạn bè xung quanh trên Mini Social.</p>
                </div>

                {/* Phần Form (Right) */}
                <div className="auth-form-wrapper">
                    <div className="auth-form-card">
                        
                        {registrationSuccess && (
                            <div style={{ backgroundColor: '#d4edda', color: '#155724', padding: '10px', borderRadius: '6px', marginBottom: '15px', fontWeight: 'bold' }}>
                                Đăng ký thành công! Vui lòng đăng nhập.
                            </div>
                        )}

                        {isRegisterMode ? (
                            // Hiển thị Form Đăng Ký
                            <RegisterForm 
                                onSuccess={handleRegistrationSuccess} 
                                onSwitchToLogin={handleSwitchToLogin}
                            />
                        ) : (
                            // Hiển thị Form Đăng Nhập
                            <>
                                <LoginForm />
                                <div className="divider"></div>
                                <div style={{ textAlign: 'center' }}>
                                    <button 
                                        className="btn-register"
                                        onClick={handleSwitchToRegister}
                                    >
                                        Tạo tài khoản mới
                                    </button>
                                </div>
                            </>
                        )}
                        
                    </div>
                    
                    {!isRegisterMode && (
                        <p className="auth-footer-text">
                            <span>Tạo Trang</span> dành cho người nổi tiếng, thương hiệu hoặc doanh nghiệp.
                        </p>
                    )}
                </div>
            </div>
        </div>
    );
};

export default AuthPage;