import React, { useState, useEffect, useRef } from 'react';
import { 
  AppBar, Toolbar, Box, InputBase, 
  IconButton, Tooltip, Button, Avatar 
} from '@mui/material';
import { styled, alpha } from '@mui/material/styles';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import SearchIcon from '@mui/icons-material/Search';
import HomeIcon from '@mui/icons-material/Home';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings'; 
import LogoutIcon from '@mui/icons-material/Logout';
import { useAuth } from '../../context/AuthContext';
import NotificationBell from '../notification/NotificationBell';
import type { User } from '../../types';
import type { Conversation } from '../../types/types';
import axiosClient from '../../api/axiosClient';
import MessengerDropdown from '../messenger/MessengerDropdown';
import { useWebSocket } from '../../context/WebSocketContext';

const Search = styled('div')(({ theme }) => ({
  position: 'relative',
  borderRadius: '20px', 
  backgroundColor: alpha(theme.palette.common.white, 0.15),
  '&:hover': {
    backgroundColor: alpha(theme.palette.common.white, 0.25),
  },
  marginLeft: theme.spacing(2),
  width: '100%',
  [theme.breakpoints.up('sm')]: {
    marginLeft: theme.spacing(3),
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
}));

const StyledInputBase = styled(InputBase)(({ theme }) => ({
  color: 'inherit',
  '& .MuiInputBase-input': {
    padding: theme.spacing(1, 1, 1, 0),
    paddingLeft: `calc(1em + ${theme.spacing(4)})`,
    transition: theme.transitions.create('width'),
    width: '100%',
    [theme.breakpoints.up('md')]: {
      width: '20ch',
    },
  },
}));

export default function Header() {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const [requests, setRequests] = useState<User[]>([]);
  const [showNotiDropdown, setShowNotiDropdown] = useState(false);
  const [showMsgDropdown, setShowMsgDropdown] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0); // State đếm tin chưa đọc

  // Refs để xử lý click ra ngoài thì đóng dropdown
  const notiRef = useRef<HTMLDivElement>(null);
  const msgRef = useRef<HTMLDivElement>(null);
  const isActive = (path: string) => location.pathname === path ? 'nav-item active' : 'nav-item';
  const { notifications } = useWebSocket();

  // --- THÊM LOGIC REAL-TIME CHO FRIEND REQUEST ---
  useEffect(() => {
    // Nếu có notification mới nhất là FRIEND_REQUEST, reload lại list
    if (notifications.length > 0) {
        const latest = notifications[0];
        if (latest.type === 'FRIEND_REQUEST' || latest.type === 'ACCEPT_FRIEND') {
            fetchRequests(); // Gọi lại API lấy lời mời
        }
        
        // Nếu có tin nhắn mới (loại Message), cập nhật lại số lượng tin nhắn chưa đọc
        // (Tuỳ vào backend em có bắn notification khi có tin nhắn không, 
        // nếu không thì giữ nguyên setInterval hoặc dùng socket chat)
        fetchUnreadCount();
    }
  }, [notifications]);

  // --- HÀM MỚI: Xử lý khi người dùng đọc 1 tin nhắn ---
  const handleMessageRead = () => {
     // Giảm số lượng đi 1, nhưng không cho âm
     setUnreadCount(prev => Math.max(0, prev - 1));
  };

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
      console.log("DANH SACH LOI MOIIIIIIIIIIIIIIIIIIIIIIIIIIIII: ", res.data)
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

  // 4. THÊM USE EFFECT NÀY: Lắng nghe Socket để auto-reload
  useEffect(() => {
    // Nếu danh sách thông báo thay đổi, kiểm tra cái mới nhất
    if (notifications.length > 0) {
        const latest = notifications[0]; // Cái mới nhất nằm đầu mảng
        
        // Nếu thông báo là về kết bạn -> Reload lại danh sách lời mời ngay
        if (latest.type === 'FRIEND_REQUEST' || latest.type === 'ACCEPT_FRIEND') {
            console.log(">>> Có biến động bạn bè, reload list request...");
            fetchRequests();
        }
    }
  }, [notifications]); // Chạy lại mỗi khi có thông báo mới

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
    <AppBar position="sticky" sx={{ 
      background: 'linear-gradient(90deg, #4F46E5 0%, #7C3AED 100%)', 
      boxShadow: '0 4px 20px 0px rgba(0, 0, 0, 0.1)' 
    }}>
      <Toolbar sx={{ justifyContent: 'space-between' }}>
        
        {/* ----- CỤM BÊN TRÁI ----- */}
        <Box sx={{ display: 'flex', alignItems: 'center' }}>
          <Box 
            onClick={() => navigate('/')} 
            sx={{ 
                cursor: 'pointer', mr: 2, fontWeight: 'bold', fontSize: '1.2rem', 
                display: 'flex', alignItems: 'center', gap: 1 
            }}
          >
            MiniSocial
          </Box>

          <Search>
            <SearchIconWrapper>
              <SearchIcon />
            </SearchIconWrapper>
            <StyledInputBase
              placeholder="Tìm kiếm..."
              inputProps={{ 'aria-label': 'search' }}
            />
          </Search>
        </Box>

        {/* ----- CỤM Ở GIỮA ----- */}
        <Box sx={{ flexGrow: 1, display: 'flex', justifyContent: 'center' }}>
          <Tooltip title="Trang chủ">
            <IconButton 
                component={RouterLink}
                to="/"
                color="inherit"
                sx={{ 
                    bgcolor: 'rgba(255,255,255,0.1)', 
                    '&:hover': { bgcolor: 'rgba(255,255,255,0.2)' }
            }}>
                <HomeIcon />
            </IconButton>
          </Tooltip>
        </Box>
        

        {/* ----- CỤM BÊN PHẢI ----- */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {isAuthenticated ? (
            <>
              {/* Nút Admin */}
              {user?.role === 'ADMIN' && (
                <Tooltip title="Trang quản trị (Dashboard)">
                  <IconButton 
                    component={RouterLink} 
                    to="/admin/dashboard" 
                    color="inherit"
                    sx={{ 
                      bgcolor: 'rgba(236, 72, 153, 0.2)', 
                      border: '1px solid rgba(236, 72, 153, 0.5)',
                      '&:hover': { bgcolor: 'rgba(236, 72, 153, 0.4)' },
                      mr: 1
                    }}
                  >
                    <AdminPanelSettingsIcon sx={{ color: '#FBCFE8' }} />
                  </IconButton>
                </Tooltip>
              )}

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
                {showMsgDropdown && (
                  <MessengerDropdown 
                      onClose={() => setShowMsgDropdown(false)} 
                      onMessageRead={handleMessageRead} // <--- TRUYỀN HÀM NÀY XUỐNG
                  />
              )}
              </div>

              <Tooltip title="Thông báo">
                  <Box> 
                     <NotificationBell />
                  </Box>
              </Tooltip>

              <Tooltip title={`Xin chào, ${user?.fullName || 'User'}`}>
                <IconButton component={RouterLink} to="/profile" sx={{ p: 0, border: '2px solid rgba(255,255,255,0.5)' }}>
                  <Avatar alt={user?.fullName} src={user?.avatarUrl || "/static/images/avatar/1.jpg"} />
                </IconButton>
              </Tooltip>

              <Tooltip title="Đăng xuất">
                <IconButton onClick={handleLogout} color="inherit">
                    <LogoutIcon />
                </IconButton>
              </Tooltip>
            </>
          ) : (
            <>
              {/* Nút Đăng nhập/Đăng ký */}
              <Button 
                color="inherit" 
                variant="text" 
                component={RouterLink} 
                to="/login"
                sx={{ mr: 1, fontWeight: 600 }}
              >
                Đăng nhập
              </Button>
              <Button 
                variant="contained" 
                component={RouterLink} 
                to="/register"
                sx={{ 
                    bgcolor: 'white', 
                    color: '#4F46E5', 
                    fontWeight: 'bold',
                    '&:hover': { bgcolor: '#F3F4F6' }
                }}
              >
                Đăng ký
              </Button>
            </>
          )}
        </Box>

      </Toolbar>
    </AppBar>
  );
}