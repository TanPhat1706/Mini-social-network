import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import axiosClient from '../api/axiosClient';
import type { User } from '../types';
import FriendRequests from './FriendRequests';
import MessengerDropdown from './MessengerDropdown'; // <--- IMPORT MỚI
import './Header.css';

const Header: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  
  const [requests, setRequests] = useState<User[]>([]);
  const [showNotiDropdown, setShowNotiDropdown] = useState(false);
  const [showMsgDropdown, setShowMsgDropdown] = useState(false); // <--- STATE MỚI
  
  const notiRef = useRef<HTMLDivElement>(null);
  const msgRef = useRef<HTMLDivElement>(null); // <--- REF MỚI

  const isActive = (path: string) => location.pathname === path ? 'nav-item active' : 'nav-item';

  const fetchRequests = async () => {
    try {
      const res = await axiosClient.get('/friends/requests');
      setRequests(res.data);
    } catch (err) { console.error(err); }
  };

  useEffect(() => { fetchRequests(); }, []);

  // Click outside to close
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (notiRef.current && !notiRef.current.contains(event.target as Node)) {
        setShowNotiDropdown(false);
      }
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
        
        {/* --- ICON MESSENGER --- */}
        <div className="nav-icon-container" ref={msgRef}>
           <div 
             className={`nav-item ${showMsgDropdown ? 'active' : ''}`}
             onClick={() => setShowMsgDropdown(!showMsgDropdown)}
             title="Tin nhắn"
             style={{fontSize: '22px'}}
           >
             💬
           </div>
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

        <div className="nav-item" title="Đăng xuất" onClick={() => {
            localStorage.removeItem('token');
            window.location.href = '/login';
        }}>🚪</div>
      </div>
    </div>
  );
};

export default Header;