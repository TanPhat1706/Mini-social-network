// src/pages/HomePage.tsx
import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api'; 
// Tạm định nghĩa IProfileData, bạn nên đặt nó trong types/auth hoặc types/user
interface IProfileData {
    id: number; 
    studentCode: string;
    email: string;
    fullName: string;
    role: string;

}

const HomePage: React.FC = () => {
    const { currentUser, logout } = useAuth(); 
    const [profileData, setProfileData] = useState<IProfileData | null>(null);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchProfile = async () => {
            // --- KHẮC PHỤC LỖI 401: Kiểm tra TOKEN trực tiếp ---
            const userJson = localStorage.getItem('user');
            if (!userJson) {
                setLoading(false);
                return; 
            }

            try {
                const response = await api.get('/user/profile'); 
                
                setProfileData(response.data);
                setError(null);

            } catch (err) {
                // KHẮC PHỤC LỖI AXIOS CATCH: 
                // Nếu Interceptor xử lý 401 thành công, response.data sẽ tồn tại.
                if (profileData) {
                    // Nếu dữ liệu đã tồn tại (từ request retry), KHÔNG hiển thị lỗi.
                    setError(null);
                } else {
                    // Nếu không có dữ liệu nào và vẫn gặp lỗi, hiển thị lỗi.
                    setError("Không thể tải thông tin cá nhân. Vui lòng thử lại.");
                }
            } finally {
                setLoading(false);
            }
        };

        fetchProfile();
    }, []); 

    if (loading) {
        return <div className="homepage-loading">Đang tải trang chủ...</div>;
    }

    const displayUser = profileData || currentUser;

    if (!displayUser) {
        // Có thể chuyển hướng đến trang login nếu không có dữ liệu nào
        return <div className="homepage-error">Bạn chưa đăng nhập.</div>;
    }

    return (
        <div className="homepage-container" style={{ padding: '20px', backgroundColor: 'white', minHeight: '100vh' }}>
            
            {/* Thanh điều hướng (Header) */}
            <div className="header-bar" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '30px', borderBottom: '1px solid #dddfe2', paddingBottom: '10px' }}>
                <h1 style={{ color: '#1877F2', fontSize: '32px', fontWeight: 'bold' }}>Mini Social</h1>
                <button 
                    onClick={logout} 
                    className="btn-logout"
                    style={{ backgroundColor: '#f0f2f5', color: 'gray', border: 'none', padding: '10px 15px', borderRadius: '6px', cursor: 'pointer', fontWeight: 'bold' }}
                >
                    Đăng xuất
                </button>
            </div>

            {/* Nội dung chính */}
            <div className="content">
                <h2 style={{ fontSize: '24px', marginBottom: '20px' }}>
                    Chào mừng trở lại, {displayUser?.fullName || displayUser?.studentCode}!
                </h2>
                
                {error && <p style={{ color: 'red' }}>{error}</p>}

                <div className="user-info-card" style={{ border: '1px solid #dddfe2', padding: '20px', borderRadius: '8px', maxWidth: '600px', margin: '0 auto', textAlign: 'left' }}>
                    <p><strong>Họ và tên:</strong> {displayUser?.fullName}</p>
                    <p><strong>Mã sinh viên:</strong> {displayUser?.studentCode}</p>
                    <p><strong>Email:</strong> {displayUser?.email}</p>
                    {displayUser?.role && <p><strong>Vai trò:</strong> {displayUser.role}</p>}
                    <p style={{ marginTop: '15px', color: 'gray', fontSize: '14px' }}>
                         Thông tin này được tải từ API bảo vệ (/api/user/profile).
                    </p>
                </div>
                
                <p style={{ marginTop: '50px', color: 'gray' }}>Đây là trang chủ của bạn. Bạn có thể thêm các tính năng mạng xã hội tại đây!</p>
            </div>
        </div>
    );
};

export default HomePage;