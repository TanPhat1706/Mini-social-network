import React, { useState, useEffect, useRef } from 'react';
import { 
  AppBar, Toolbar, Box, InputBase, 
  IconButton, Tooltip, Button, Avatar, Badge, Typography
} from '@mui/material';
import { styled, alpha } from '@mui/material/styles';
import { Link as RouterLink, useNavigate, useLocation } from 'react-router-dom';
import SearchIcon from '@mui/icons-material/Search';
import HomeIcon from '@mui/icons-material/Home';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings'; 
import LogoutIcon from '@mui/icons-material/Logout';
import NotificationsIcon from '@mui/icons-material/Notifications';
import ChatBubbleIcon from '@mui/icons-material/ChatBubble';
import { useAuth } from '../../context/AuthContext';
import NotificationBell from '../notification/NotificationBell';
import type { User } from '../../types';
import type { Conversation } from '../../types/types';
import axiosClient from '../../api/axiosClient';
import MessengerDropdown from '../messenger/MessengerDropdown';
import { useWebSocket } from '../../context/WebSocketContext';
import { FaFacebookMessenger } from "react-icons/fa";

// --- STYLED COMPONENTS (FACEBOOK STYLE) ---
const Search = styled('div')(({ theme }) => ({
  position: 'relative',
  borderRadius: '20px', 
  backgroundColor: '#F0F2F5', // Màu xám nhạt cho thanh tìm kiếm trên nền trắng
  '&:hover': {
    backgroundColor: '#E4E6E9',
  },
  marginLeft: theme.spacing(1),
  width: '100%',
  [theme.breakpoints.up('sm')]: {
    width: 'auto',
  },
}));

const SearchIconWrapper = styled('div')(({ theme }) => ({
  padding: theme.spacing(0, 2),
  height: '100%',
  position: 'absolute',
  pointerEvents: 'none',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  color: '#65676B',
}));

const StyledInputBase = styled(InputBase)(({ theme }) => ({
  color: '#050505',
  '& .MuiInputBase-input': {
    padding: theme.spacing(1, 1, 1, 0),
    paddingLeft: `calc(1em + ${theme.spacing(4)})`,
    width: '100%',
    [theme.breakpoints.up('md')]: {
      width: '25ch',
    },
  },
}));

const ActionIconButton = styled(IconButton)(({ theme }) => ({
  backgroundColor: '#E4E6E9',
  color: '#050505',
  '&:hover': {
    backgroundColor: '#D8DADF',
  },
  width: '40px',
  height: '40px',
}));

export default function Header() {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // --- GIỮ NGUYÊN TOÀN BỘ LOGIC CODE CŨ ---
  const [requests, setRequests] = useState<User[]>([]);
  const [showNotiDropdown, setShowNotiDropdown] = useState(false);
  const [showMsgDropdown, setShowMsgDropdown] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

  const notiRef = useRef<HTMLDivElement>(null);
  const msgRef = useRef<HTMLDivElement>(null);
  const { notifications } = useWebSocket();

  const handleLogout = () => { logout(); navigate('/login'); };

  useEffect(() => {
    if (notifications.length > 0) {
        const latest = notifications[0];
        if (latest.type === 'FRIEND_REQUEST' || latest.type === 'ACCEPT_FRIEND') {
            fetchRequests();
        }
        fetchUnreadCount();
    }
  }, [notifications]);

  const fetchUnreadCount = async () => {
    try {
      const res = await axiosClient.get('/messages/recent');
      const convs: Conversation[] = res.data;
      const count = convs.filter(c => c.isRead === false).length;
      setUnreadCount(count);
    } catch (err) { console.error("Lỗi lấy tin nhắn:", err); }
  };

  const fetchRequests = async () => {
    try {
      const res = await axiosClient.get('/friends/requests');
      setRequests(res.data);
    } catch (err) { console.error(err); }
  };

  useEffect(() => {
    fetchRequests();
    fetchUnreadCount();
    const interval = setInterval(fetchUnreadCount, 5000);
    return () => clearInterval(interval);
  }, []);

  const handleMessageRead = () => { setUnreadCount(prev => Math.max(0, prev - 1)); };

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (notiRef.current && !notiRef.current.contains(event.target as Node)) setShowNotiDropdown(false);
      if (msgRef.current && !msgRef.current.contains(event.target as Node)) setShowMsgDropdown(false);
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <AppBar position="sticky" sx={{ 
      backgroundColor: '#FFFFFF', 
      color: '#050505', 
      boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
      zIndex: 1100 
    }}>
      <Toolbar sx={{ justifyContent: 'space-between', minHeight: '56px !important' }}>
        
        {/* LOGO & SEARCH */}
        <Box sx={{ display: 'flex', alignItems: 'center', flex: 1 }}>
          <Box 
            onClick={() => navigate('/')} 
            sx={{ cursor: 'pointer', display: 'flex', alignItems: 'center' }}
          >
            {/* THAY THẾ TEXT BẰNG LOGO.PNG */}
            <img src="/logo.png" alt="Logo" style={{ height: '40px', width: '40px' }} />
          </Box>

          <Search>
            <SearchIconWrapper><SearchIcon /></SearchIconWrapper>
            <StyledInputBase placeholder="Tìm kiếm trên MiniSocial" />
          </Search>
        </Box>

        {/* CỤM GIỮA: NAVIGATION ICONS */}
        <Box sx={{ display: { xs: 'none', md: 'flex' }, flex: 1, justifyContent: 'center' }}>
          <Tooltip title="Trang chủ">
            <IconButton 
                component={RouterLink} to="/"
                sx={{ 
                  borderRadius: '0px', padding: '10px 30px',
                  color: location.pathname === '/' ? '#1877F2' : '#65676B',
                  borderBottom: location.pathname === '/' ? '3px solid #1877F2' : '3px solid transparent'
                }}
            >
                <HomeIcon fontSize="large" />
            </IconButton>
          </Tooltip>
        </Box>
        

        {/* CỤM PHẢI: ACTIONS */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flex: 1, justifyContent: 'flex-end' }}>
          {isAuthenticated ? (
            <>
              {user?.role === 'ADMIN' && (
                <Tooltip title="Trang quản trị">
                  <ActionIconButton onClick={() => navigate('/admin/dashboard')}>
                    <AdminPanelSettingsIcon />
                  </ActionIconButton>
                </Tooltip>
              )}

<<<<<<< HEAD
              {/* MESSENGER */}
              <Box sx={{ position: 'relative' }} ref={msgRef}>
                <Tooltip title="Tin nhắn">
                  <ActionIconButton onClick={() => setShowMsgDropdown(!showMsgDropdown)}>
                    <Badge badgeContent={unreadCount} color="error" max={9}>
                      <ChatBubbleIcon />
                    </Badge>
                  </ActionIconButton>
                </Tooltip>
=======
              {/* --- ICON MESSENGER (CÓ SỐ ĐỎ) --- */}
              <div className="nav-icon-container" ref={msgRef}>
  <div 
    className={`nav-item ${showMsgDropdown ? 'active' : ''}`}
    onClick={() => {
      setShowMsgDropdown(!showMsgDropdown);
      if (!showMsgDropdown) fetchUnreadCount();
    }}
    title="Tin nhắn"
    style={{ fontSize: '22px', position: 'relative' }}
  >
    <FaFacebookMessenger />

    {unreadCount > 0 && (
      <span className="badge">
        {unreadCount > 9 ? '9+' : unreadCount}
      </span>
    )}
  </div>
</div>

                
                {/* Dropdown Component */}
>>>>>>> ca972e86f39c6f8df2699f5e4e10409b461eab65
                {showMsgDropdown && (
                  <MessengerDropdown onClose={() => setShowMsgDropdown(false)} onMessageRead={handleMessageRead} />
                )}
              </Box>

              {/* NOTIFICATIONS */}
              <Box ref={notiRef}>
                <NotificationBell />
              </Box>

              {/* PROFILE */}
              <Tooltip title={user?.fullName || 'Tài khoản'}>
                <Box 
                  onClick={() => navigate('/profile')}
                  sx={{ 
                    display: 'flex', alignItems: 'center', gap: 1, cursor: 'pointer',
                    ml: 1, p: '4px 8px', borderRadius: '20px', '&:hover': { bgcolor: '#F2F2F2' }
                  }}
                >
                  <Avatar sx={{ width: 28, height: 28 }} src={user?.avatarUrl} alt={user?.fullName} />
                  <Typography variant="body2" sx={{ fontWeight: 600, display: { xs: 'none', lg: 'block' } }}>
                    {user?.fullName?.split(' ').pop()}
                  </Typography>
                </Box>
              </Tooltip>

              <Tooltip title="Đăng xuất">
                <ActionIconButton onClick={handleLogout}><LogoutIcon /></ActionIconButton>
              </Tooltip>
            </>
          ) : (
            <Button variant="contained" onClick={() => navigate('/login')}>Đăng nhập</Button>
          )}
        </Box>
      </Toolbar>
    </AppBar>
  );
}