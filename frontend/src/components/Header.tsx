import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import axiosClient from '../api/axiosClient';
import type { User } from '../types';
import FriendRequests from './FriendRequests';
import MessengerDropdown from './MessengerDropdown';
import './Header.css';

interface Conversation {
  partnerId: number;
  partnerName: string;
  partnerAvatar: string;
  lastMessage: string;
  timestamp: string;
  isRead: boolean;
}

const Header: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  
  // State quản lý dữ liệu
  const [requests, setRequests] = useState<User[]>([]);
  const [showNotiDropdown, setShowNotiDropdown] = useState(false);
  const [showMsgDropdown, setShowMsgDropdown] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0); // State đếm tin chưa đọc

  // Refs để xử lý click ra ngoài thì đóng dropdown
  const notiRef = useRef<HTMLDivElement>(null);
  const msgRef = useRef<HTMLDivElement>(null);

  const isActive = (path: string) => location.pathname === path ? 'nav-item active' : 'nav-item';

  // --- 1. HÀM LẤY SỐ TIN NHẮN CHƯA ĐỌC ---
  const fetchUnreadCount = async () => {
    try {
      const res = await axiosClient.get('/messages/recent');
      const convs: Conversation[] = res.data;
      // Đếm những cuộc hội thoại có isRead = false
      const count = convs.filter(c => c.isRead === false).length;
      setUnreadCount(count);
    } catch (err) {
      console.error("Lỗi lấy tin nhắn:", err);
    }
  };

  // --- 2. HÀM LẤY LỜI MỜI KẾT BẠN (Giữ nguyên logic cũ của bạn) ---
  const fetchRequests = async () => {
    try {
      const res = await axiosClient.get('/friends/requests');
      setRequests(res.data);
    } catch (err) { console.error(err); }
  };

  // --- 3. USE EFFECT: CHẠY KHI MỞ APP ---
  useEffect(() => {
    fetchRequests();
    fetchUnreadCount();

    // Tự động cập nhật số tin nhắn mỗi 5 giây (Polling đơn giản)
    // Nếu bạn đã làm WebSocket update realtime thì có thể bỏ dòng này
    const interval = setInterval(fetchUnreadCount, 5000);
    return () => clearInterval(interval);
  }, []);

  // --- 4. USE EFFECT: XỬ LÝ CLICK RA NGOÀI (CLOSE DROPDOWN) ---
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      // Đóng dropdown thông báo
      if (notiRef.current && !notiRef.current.contains(event.target as Node)) {
        setShowNotiDropdown(false);
      }
      // Đóng dropdown tin nhắn
      if (msgRef.current && !msgRef.current.contains(event.target as Node)) {
        setShowMsgDropdown(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <div className="header">
      <div style={{display: 'flex', alignItems: 'center', gap: '20px'}}>
        <div className="brand-logo" onClick={() => navigate('/')}>MiniSocial</div>
        <input className="search-bar" placeholder="Tìm kiếm bạn bè..." />
      </div>
      
      <div className="nav-group">
        <div className={isActive('/')} onClick={() => navigate('/')} title="Trang chủ">🏠</div>
        <div className={isActive('/profile')} onClick={() => navigate('/profile')} title="Hồ sơ">👤</div>
        
        {/* --- ICON MESSENGER (CÓ SỐ ĐỎ) --- */}
        <div className="nav-icon-container" ref={msgRef}>
           <div 
             className={`nav-item ${showMsgDropdown ? 'active' : ''}`}
             onClick={() => {
               setShowMsgDropdown(!showMsgDropdown);
               // Khi mở ra thì fetch lại để đảm bảo số liệu đúng nhất
               if (!showMsgDropdown) fetchUnreadCount();
             }}
             title="Tin nhắn"
             style={{fontSize: '22px', position: 'relative'}}
           >
             💬
             {/* Logic hiển thị Badge số đỏ */}
             {unreadCount > 0 && (
               <span className="badge">
                 {unreadCount > 9 ? '9+' : unreadCount}
               </span>
             )}
           </div>
           
           {/* Dropdown Component */}
           {showMsgDropdown && <MessengerDropdown onClose={() => setShowMsgDropdown(false)} />}
        </div>

        {/* --- ICON THÔNG BÁO --- */}
        <div className="nav-icon-container" ref={notiRef}>
          <div 
            className={`nav-item ${showNotiDropdown ? 'active' : ''}`} 
            onClick={() => setShowNotiDropdown(!showNotiDropdown)}
          >
            🔔
            {requests.length > 0 && <span className="badge">{requests.length}</span>}
          </div>
          {showNotiDropdown && (
            <div className="notification-dropdown">
              <FriendRequests requests={requests} onRequestChanged={fetchRequests} />
            </div>
          )}
        </div>

        {/* --- NÚT ĐĂNG XUẤT --- */}
        <div className="nav-item" title="Đăng xuất" onClick={() => {
            localStorage.removeItem('token');
            window.location.href = '/login';
        }}>🚪</div>
      </div>
    </div>
  );
};

export default Header;