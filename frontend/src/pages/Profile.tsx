import React, { useEffect, useState } from 'react';
import axiosClient from '../api/axiosClient';
import { useAuth } from '../context/AuthContext';
import type { User } from '../types';

const Profile: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const { logout } = useAuth();

  useEffect(() => {
    axiosClient.get('/profile')
      .then(res => setUser(res.data))
      .catch(() => logout());
  }, [logout]);

  if (!user) return <div style={{textAlign: 'center', marginTop: '100px', color: '#666'}}>Đang tải dữ liệu...</div>;

  return (
    <div className="profile-wrapper">
      <div className="profile-card">
        {/* Ảnh bìa */}
        <div className="profile-cover"></div>

        {/* Avatar */}
        <div className="profile-avatar-container">
          <img 
            className="profile-avatar"
            src={user.avatarUrl || "https://api.dicebear.com/7.x/avataaars/svg?seed=" + user.studentCode} 
            alt="Avatar" 
          />
        </div>

        {/* Thông tin chính */}
        <div className="profile-info">
          <h2 className="profile-name">{user.fullName}</h2>
          <span className="profile-role">{user.role}</span>
          <p style={{ marginTop: '10px', color: '#666', fontStyle: 'italic' }}>
            "{user.bio || 'Chưa cập nhật tiểu sử'}"
          </p>

          {/* Thống kê giả lập cho đẹp */}
          <div className="profile-stats">
            <div className="stat-item">
              <h4>Active</h4>
              <p>Status</p>
            </div>
            <div className="stat-item">
              <h4>{user.className || 'N/A'}</h4>
              <p>Lớp</p>
            </div>
            <div className="stat-item">
              <h4>SV</h4>
              <p>Hệ</p>
            </div>
          </div>

          {/* Grid chi tiết thông tin */}
          <div className="profile-details-grid">
            <div className="detail-box">
              <div className="detail-label">Mã Sinh Viên</div>
              <div className="detail-value">{user.studentCode}</div>
            </div>
            <div className="detail-box">
              <div className="detail-label">Email Liên Hệ</div>
              <div className="detail-value">{user.email}</div>
            </div>
            <div className="detail-box">
              <div className="detail-label">Ngày tham gia</div>
              <div className="detail-value">{new Date(user.createdAt).toLocaleDateString()}</div>
            </div>
            <div className="detail-box">
              <div className="detail-label">Đăng nhập lần cuối</div>
              <div className="detail-value">{new Date(user.lastLogin).toLocaleString()}</div>
            </div>
          </div>

          <button onClick={logout} className="btn-logout">Đăng xuất</button>
        </div>
      </div>
    </div>
  );
};

export default Profile;