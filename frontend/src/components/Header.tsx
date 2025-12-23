import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import axiosClient from '../api/axiosClient';
import type { User } from '../types';
import FriendRequests from './FriendRequests';
import './Header.css';

const Header: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  
  // State quản lý danh sách lời mời & đóng mở dropdown
  const [requests, setRequests] = useState<User[]>([]);
  const [showDropdown, setShowDropdown] = useState(false);
  
  // Ref để xử lý click outside (bấm ra ngoài thì đóng dropdown)
  const dropdownRef = useRef<HTMLDivElement>(null);

  const isActive = (path: string) => location.pathname === path ? 'nav-item active' : 'nav-item';

  // Hàm tải dữ liệu lời mời
  const fetchRequests = async () => {
    try {
      const res = await axiosClient.get('/friends/requests');
      setRequests(res.data);
    } catch (err) { console.error(err); }
  };

  useEffect(() => {
    fetchRequests(); // Gọi ngay khi mount
    
    // Có thể set interval để auto-refresh mỗi 30s nếu muốn realtime
    // const interval = setInterval(fetchRequests, 30000);
    // return () => clearInterval(interval);
  }, []);

  // Đóng dropdown khi click ra ngoài
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setShowDropdown(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [dropdownRef]);

  return (
    <div className="header">
      <div style={{display: 'flex', alignItems: 'center', gap: '20px'}}>
        <div className="brand-logo" onClick={() => navigate('/')}>MiniSocial</div>
        <input className="search-bar" placeholder="Tìm kiếm bạn bè, bài viết..." />
      </div>
      
      <div className="nav-group">
        <div className={isActive('/')} onClick={() => navigate('/')} title="Trang chủ">🏠</div>
        <div className={isActive('/profile')} onClick={() => navigate('/profile')} title="Hồ sơ">👤</div>
        
        {/* --- ICON THÔNG BÁO (Chuông) --- */}
        <div className="nav-icon-container" ref={dropdownRef}>
          <div 
            className={`nav-item ${showDropdown ? 'active' : ''}`} 
            title="Thông báo / Lời mời kết bạn"
            onClick={() => setShowDropdown(!showDropdown)}
          >
            🔔
            {/* Nếu có lời mời thì hiện Badge đỏ */}
            {requests.length > 0 && <span className="badge">{requests.length}</span>}
          </div>

          {/* DROPDOWN MENU */}
          {showDropdown && (
            <div className="notification-dropdown">
              <FriendRequests 
                requests={requests} 
                onRequestChanged={fetchRequests} // Khi accept/delete xong thì reload lại list
              />
            </div>
          )}
        </div>
        {/* -------------------------------- */}

        <div className="nav-item" title="Đăng xuất" onClick={() => {
            localStorage.removeItem('token');
            window.location.href = '/login';
        }}>🚪</div>
      </div>
    </div>
  );
};

export default Header;