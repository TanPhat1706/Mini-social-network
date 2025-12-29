import React, { useState, useEffect, useRef } from 'react';
import { 
  AppBar, Toolbar, Box, InputBase, Paper, List, ListItem,
  IconButton, Tooltip, Button, Avatar, Badge, Typography, CircularProgress
} from '@mui/material';
import { styled, alpha } from '@mui/material/styles';
import { useNavigate, useLocation, Link as RouterLink } from 'react-router-dom';

// Import Icons
import SearchIcon from '@mui/icons-material/Search';
import HomeIcon from '@mui/icons-material/Home';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings'; 
import LogoutIcon from '@mui/icons-material/Logout';
import ChatBubbleIcon from '@mui/icons-material/ChatBubble';

// Import Logic & Components
import { useAuth } from '../../context/AuthContext';
import NotificationBell from '../notification/NotificationBell';
import type { User } from '../../types';
import type { Conversation } from '../../types/types';
import axiosClient from '../../api/axiosClient';
import MessengerDropdown from '../messenger/MessengerDropdown';
import { useWebSocket } from '../../context/WebSocketContext';
import FriendButton from '../friend/FriendButton';

// --- STYLED COMPONENTS (FACEBOOK STYLE) ---
const Search = styled('div')(({ theme }) => ({
  position: 'relative',
  borderRadius: '20px', 
  backgroundColor: '#F0F2F5',
  '&:hover': { backgroundColor: '#E4E6E9' },
  marginLeft: theme.spacing(1),
  width: '100%',
  [theme.breakpoints.up('sm')]: { width: 'auto' },
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
  color: 'inherit',
  '& .MuiInputBase-input': {
    padding: theme.spacing(1, 1, 1, 0),
    paddingLeft: `calc(1em + ${theme.spacing(4)})`,
    width: '100%',
    [theme.breakpoints.up('md')]: { width: '25ch' },
  },
}));

const SearchDropdown = styled(Paper)(({ theme }) => ({
  position: 'absolute',
  top: '100%',
  left: 0,
  right: 0,
  marginTop: '8px',
  maxHeight: '450px',
  overflowY: 'auto',
  zIndex: 1300,
  boxShadow: '0 12px 28px 0 rgba(0, 0, 0, 0.2), 0 2px 4px 0 rgba(0, 0, 0, 0.1)',
  borderRadius: '8px',
  width: '360px',
  [theme.breakpoints.down('sm')]: { width: '100%' },
}));

const NavIconButton = styled(IconButton)<{ active?: boolean }>(({ theme, active }) => ({
  borderRadius: '0px',
  padding: '10px 30px',
  color: active ? theme.palette.primary.main : '#65676B',
  borderBottom: active ? `3px solid ${theme.palette.primary.main}` : '3px solid transparent',
  '&:hover': { backgroundColor: '#F2F2F2' },
  [theme.breakpoints.down('md')]: { padding: '10px 15px' },
}));

const ActionIconButton = styled(IconButton)(({ theme }) => ({
  backgroundColor: '#E4E6E9',
  color: '#050505',
  '&:hover': { backgroundColor: '#D8DADF' },
  width: '40px',
  height: '40px',
}));
export default function Header() {
  // --- AUTH & NAVIGATION ---
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // --- STATE TÌM KIẾM ---
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<User[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [showSearchDropdown, setShowSearchDropdown] = useState(false);
  const searchRef = useRef<HTMLDivElement>(null);

  // --- STATE THÔNG BÁO & TIN NHẮN ---
  const [requests, setRequests] = useState<User[]>([]);
  const [showMsgDropdown, setShowMsgDropdown] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const msgRef = useRef<HTMLDivElement>(null);
  const notiRef = useRef<HTMLDivElement>(null);
  const { notifications } = useWebSocket();

  // --- LOGIC TÌM KIẾM (DEBOUNCE 500MS) ---
  useEffect(() => {
    if (!searchQuery.trim()) {
      setSearchResults([]);
      setShowSearchDropdown(false);
      return;
    }

    const delayDebounce = setTimeout(async () => {
      setIsSearching(true);
      setShowSearchDropdown(true);
      try {
        const res = await axiosClient.get(`/search?name=${searchQuery}`);
        setSearchResults(res.data);
      } catch (err) {
        console.error("Lỗi tìm kiếm:", err);
      } finally {
        setIsSearching(false);
      }
    }, 500);

    return () => clearTimeout(delayDebounce);
  }, [searchQuery]);

  // --- LOGIC REAL-TIME (WEBSOCKET) ---
  useEffect(() => {
    if (notifications.length > 0) {
        const latest = notifications[0];
        if (latest.type === 'FRIEND_REQUEST' || latest.type === 'ACCEPT_FRIEND') {
            fetchRequests();
        }
        fetchUnreadCount();
    }
  }, [notifications]);

  // --- LOGIC FETCH DỮ LIỆU ĐỊNH KỲ ---
  const fetchUnreadCount = async () => {
    try {
      const res = await axiosClient.get('/messages/recent');
      const convs: Conversation[] = res.data;
      const count = convs.filter(c => c.isRead === false).length;
      setUnreadCount(count);
    } catch (err) {
      console.error("Lỗi lấy tin nhắn:", err);
    }
  };

  const fetchRequests = async () => {
    try {
      const res = await axiosClient.get('/friends/requests');
      setRequests(res.data);
    } catch (err) { console.error(err); }
  };

  useEffect(() => {
    if (isAuthenticated) {
        fetchRequests();
        fetchUnreadCount();
        const interval = setInterval(fetchUnreadCount, 5000);
        return () => clearInterval(interval);
    }
  }, [isAuthenticated]);

  // --- XỬ LÝ ĐÓNG DROPDOWN KHI CLICK NGOÀI ---
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (msgRef.current && !msgRef.current.contains(event.target as Node)) {
        setShowMsgDropdown(false);
      }
      if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
setShowSearchDropdown(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleLogout = () => { logout(); navigate('/login'); };
  const handleMessageRead = () => { setUnreadCount(prev => Math.max(0, prev - 1)); };

  return (
    <AppBar position="sticky" sx={{ backgroundColor: '#FFFFFF', color: '#050505', boxShadow: '0 2px 4px rgba(0,0,0,0.1)', zIndex: 1100 }}>
      <Toolbar sx={{ justifyContent: 'space-between', minHeight: '56px !important' }}>
        
        {/* --- VÙNG TRÁI: LOGO & SEARCH --- */}
        <Box sx={{ display: 'flex', alignItems: 'center', flex: 1, position: 'relative' }} ref={searchRef}>
          <Box onClick={() => navigate('/')} sx={{ cursor: 'pointer', display: 'flex', alignItems: 'center', mr: 1 }}>
            <img src="/logo.png" alt="Logo" style={{ height: '40px', width: '40px' }} />
          </Box>
          
          <Search>
            <SearchIconWrapper><SearchIcon /></SearchIconWrapper>
            <StyledInputBase 
              placeholder="Tìm kiếm sinh viên..." 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onFocus={() => searchQuery && setShowSearchDropdown(true)}
            />

            {/* DROPDOWN KẾT QUẢ TÌM KIẾM */}
            {showSearchDropdown && (
              <SearchDropdown>
                <Box sx={{ p: 1.5, borderBottom: '1px solid #ddd' }}>
                  <Typography variant="subtitle2" fontWeight={700} color="text.secondary">Kết quả tìm kiếm</Typography>
                </Box>
                <List sx={{ py: 0 }}>
                  {isSearching ? (
                    <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}><CircularProgress size={28} /></Box>
                  ) : searchResults.length > 0 ? (
                    searchResults.map((result) => (
                      <ListItem 
                        key={result.id}
                        sx={{ 
                          display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                          gap: 1, '&:hover': { bgcolor: '#F2F2F2' }, py: 1, px: 1.5
                        }}
                      >
                        <Box 
                          sx={{ display: 'flex', alignItems: 'center', gap: 1.5, cursor: 'pointer', flex: 1, minWidth: 0 }}
                          onClick={() => { navigate(`/profile/${result.id}`); setShowSearchDropdown(false); }}
                        >
                          <Avatar src={result.avatarUrl} sx={{ width: 40, height: 40 }} />
                          <Box sx={{ overflow: 'hidden' }}>
                            <Typography variant="body2" fontWeight={600} noWrap>{result.fullName}</Typography>
                            <Typography variant="caption" color="text.secondary" noWrap>{result.studentCode}</Typography>
</Box>
                        </Box>

                        {/* HIỆN TRẠNG THÁI QUAN HỆ QUA FRIENDBUTTON */}
                        <Box sx={{ width: '115px', flexShrink: 0 }}>
                          {user && result.id !== user.id && (
                            <FriendButton 
                              targetUserId={result.id} 
                              currentUserId={user.id} 
                            />
                          )}
                        </Box>
                      </ListItem>
                    ))
                  ) : (
                    <Box sx={{ p: 4, textAlign: 'center' }}>
                      <Typography variant="body2" color="text.secondary">Không tìm thấy sinh viên này.</Typography>
                    </Box>
                  )}
                </List>
              </SearchDropdown>
            )}
          </Search>
        </Box>

        {/* --- VÙNG GIỮA: NAV ICONS --- */}
        <Box sx={{ display: { xs: 'none', md: 'flex' }, flex: 1, justifyContent: 'center', height: '56px' }}>
          <NavIconButton active={location.pathname === '/'} onClick={() => navigate('/')}>
            <HomeIcon fontSize="large" />
          </NavIconButton>
        </Box>

        {/* --- VÙNG PHẢI: ACTIONS --- */}
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

              {/* MESSENGER DROPDOWN */}
              <Box sx={{ position: 'relative' }} ref={msgRef}>
                <Tooltip title="Tin nhắn">
                  <ActionIconButton onClick={() => setShowMsgDropdown(!showMsgDropdown)}>
                    <Badge badgeContent={unreadCount} color="error" max={9}>
                      <ChatBubbleIcon />
                    </Badge>
                  </ActionIconButton>
                </Tooltip>
                {showMsgDropdown && (
                  <MessengerDropdown onClose={() => setShowMsgDropdown(false)} onMessageRead={handleMessageRead} />
                )}
              </Box>

              {/* NOTIFICATIONS BELL */}
              <Box ref={notiRef}>
                <NotificationBell />
              </Box>

              {/* USER PROFILE BOX */}
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

              {/* LOGOUT BUTTON */}
              <Tooltip title="Đăng xuất">
                <ActionIconButton onClick={handleLogout}><LogoutIcon /></ActionIconButton>
              </Tooltip>
            </>
          ) : (
            <Button variant="contained" color="primary" onClick={() => navigate('/login')}>
              Đăng nhập
            </Button>
          )}
        </Box>
      </Toolbar>
    </AppBar>
  );
}
