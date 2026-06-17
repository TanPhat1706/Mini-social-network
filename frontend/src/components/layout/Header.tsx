import { useState, useEffect, useRef } from 'react';
import {
  AppBar, Toolbar, Box, InputBase, Paper, List, ListItem,
  IconButton, Tooltip, Button, Badge, Typography, CircularProgress,
  Menu, MenuItem, ListItemIcon, Divider
} from '@mui/material';
import { styled } from '@mui/material/styles';
import { useNavigate, useLocation } from 'react-router-dom';
import { useColorMode } from '../../styles/theme';

// Import Icons
import SecurityIcon from '@mui/icons-material/Security';
import DarkModeIcon from '@mui/icons-material/DarkMode';
import SearchIcon from '@mui/icons-material/Search';
import HomeIcon from '@mui/icons-material/Home';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import LogoutIcon from '@mui/icons-material/Logout';
import ChatBubbleIcon from '@mui/icons-material/ChatBubble';
import SettingsIcon from '@mui/icons-material/Settings';
import LockResetIcon from '@mui/icons-material/LockReset';

// Import Logic & Components
import { useAuth } from '../../context/AuthContext';
import NotificationBell from '../notification/NotificationBell';
import type { User } from '../../types';
import axiosClient from '../../api/axiosClient';
import MessengerDropdown from '../messenger/MessengerDropdown';
import { useWebSocket } from '../../context/useWebSocket';
import FriendButton from '../friend/FriendButton';
import ChangePasswordModal from '../auth/ChangePasswordModal';
import { useProfileNavigation } from '../../utils/useProfileNavigation';

// 🔴 IMPORT COMPONENT AVATAR MA THUẬT
import AvatarWithFrame from '../AvatarWithFrame';
import ColoredName from '../ColoredName';

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
  zIndex: 1300,
  boxShadow: '0 12px 28px 0 rgba(0, 0, 0, 0.2), 0 2px 4px 0 rgba(0, 0, 0, 0.1)',
  borderRadius: '8px',
  width: '360px',
  [theme.breakpoints.down('sm')]: { width: '100%' },

  /* --- CÁCH 1: ẨN HOÀN TOÀN THANH CUỘN NHƯNG VẪN CHO PHÉP CUỘN --- */
  overflowY: 'auto',
  
  // Dành cho Chrome, Edge, Safari
  '&::-webkit-scrollbar': {
    display: 'none', 
    width: '0px',
  },
  
  // Dành cho Firefox
  scrollbarWidth: 'none',
  
  // Dành cho IE / Edge cũ
  msOverflowStyle: 'none',
}));

const NavIconButton = styled(IconButton)<{ active?: boolean }>(({ theme, active }) => ({
  borderRadius: '0px',
  padding: '10px 30px',
  color: active ? theme.palette.primary.main : '#65676B',
  borderBottom: active ? `3px solid ${theme.palette.primary.main}` : '3px solid transparent',
  '&:hover': { backgroundColor: '#F2F2F2' },
  [theme.breakpoints.down('md')]: { padding: '10px 15px' },
}));

const ActionIconButton = styled(IconButton)(() => ({
  backgroundColor: '#E4E6E9',
  color: '#050505',
  '&:hover': { backgroundColor: '#D8DADF' },
  width: '40px',
  height: '40px',
}));

export default function Header() {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { toggleColorMode, mode } = useColorMode();
  const navigateToProfile = useProfileNavigation();
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<User[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [showSearchDropdown, setShowSearchDropdown] = useState(false);
  const searchRef = useRef<HTMLDivElement>(null);

  const [showMsgDropdown, setShowMsgDropdown] = useState(false);
  const [unreadMessageCount, setUnreadMessageCount] = useState(0);
  const msgRef = useRef<HTMLDivElement>(null);
  const notiRef = useRef<HTMLDivElement>(null);
  const { notifications } = useWebSocket();
  const [liveUser, setLiveUser] = useState<User | null>(null);

  // 🟢 HỢP NHẤT STATE CHO SETTINGS MENU & ĐỔI MẬT KHẨU
  const [settingsAnchorEl, setSettingsAnchorEl] = useState<null | HTMLElement>(null);
  const [openChangePassword, setOpenChangePassword] = useState(false);
  const openSettings = Boolean(settingsAnchorEl);

  useEffect(() => {
    const trimmedQuery = searchQuery.trim();

    if (!trimmedQuery) {
      setSearchResults([]);
      setShowSearchDropdown(false);
      return;
    }

    let ignore = false;
    const delayDebounce = setTimeout(async () => {
      setIsSearching(true);
      setShowSearchDropdown(true);
      try {
        const res = await axiosClient.get('/search', {
          params: { name: trimmedQuery },
        });
        const data = res.data;
        const normalizedResults =
          data?.content ||
          data?.data ||
          (Array.isArray(data) ? data : []);

        if (!ignore) {
          setSearchResults(normalizedResults);
        }
      } catch (err) {
        console.error("Lỗi tìm kiếm:", err);
        if (!ignore) {
          setSearchResults([]);
        }
      } finally {
        if (!ignore) {
          setIsSearching(false);
        }
      }
    }, 500);

    return () => {
      ignore = true;
      clearTimeout(delayDebounce);
      setIsSearching(false);
    };
  }, [searchQuery]);

  const fetchUnreadMessageCount = async () => {
    try {
      const res = await axiosClient.get('/messages/recent');
      const data = res.data;
      const convs =
        data?.content ||
        data?.data ||
        data?.conversations ||
        (Array.isArray(data) ? data : []);

      const count = Array.isArray(convs)
        ? convs.filter(c => c.isRead === false).length
        : 0;

      setUnreadMessageCount(count);
    } catch (err) {
      console.error("Lỗi lấy tin nhắn:", err);
    }
  };

  useEffect(() => {
    if (isAuthenticated) {
      fetchUnreadMessageCount();
      axiosClient.get('/profile')
        .then(res => setLiveUser(res.data))
        .catch(console.error);
      const interval = setInterval(fetchUnreadMessageCount, 5000);
      return () => clearInterval(interval);
    }
  }, [isAuthenticated]);

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

  const handleMessageRead = () => { setUnreadMessageCount(prev => Math.max(0, prev - 1)); };

  // 🟢 HỢP NHẤT HÀM XỬ LÝ MENU SETTINGS
  const handleSettingsClick = (event: React.MouseEvent<HTMLElement>) => {
    setSettingsAnchorEl(event.currentTarget);
  };

  const handleSettingsClose = () => {
    setSettingsAnchorEl(null);
  };

  const handleOpenChangePassword = () => {
    handleSettingsClose();
    setOpenChangePassword(true);
  };

  const handleLogout = () => {
    handleSettingsClose();
    logout();
    navigate('/login');
  };
  // 🟢 HÀM XỬ LÝ KHI CLICK VÀO HOME HOẶC LOGO
  const handleHomeClick = () => {
    if (location.pathname === '/') {
      // 1. Nếu ĐÃ Ở TRANG CHỦ -> Cuộn mượt lên trên cùng
      window.scrollTo({
        top: 0,
        behavior: 'smooth'
      });

      // 2. Bắn một sự kiện cục bộ để báo cho Feed component biết cần lấy lại dữ liệu mới
      window.dispatchEvent(new CustomEvent('refreshFeed'));
    } else {
      // Nếu ĐANG Ở TRANG KHÁC -> Điều hướng về trang chủ
      navigate('/');
    }
  };

  return (
    <AppBar position="sticky" sx={{ backgroundColor: '#FFFFFF', color: '#050505', boxShadow: '0 2px 4px rgba(0,0,0,0.1)', zIndex: 1100 }}>
      <Toolbar sx={{ justifyContent: 'space-between', minHeight: '56px !important' }}>

        {/* --- VÙNG TRÁI: LOGO & SEARCH --- */}
        <Box sx={{ display: 'flex', alignItems: 'center', flex: 1, position: 'relative' }} ref={searchRef}>
          <Box onClick={handleHomeClick} sx={{ cursor: 'pointer', display: 'flex', alignItems: 'center', mr: 1 }}>
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
                          onClick={() => { navigateToProfile(result.studentCode); setShowSearchDropdown(false); }}
                        >
                          <AvatarWithFrame
                            src={result.avatarUrl}
                            name={result.fullName}
                            frameClass={(result as any).currentAvatarFrame}
                            size={40}
                          />
                          <Box sx={{ overflow: 'hidden' }}>
                            <Typography variant="body2" fontWeight={600} noWrap>
                              <ColoredName name={result.fullName} colorClass={(result as any).currentNameColor} />
                            </Typography>
                            <Typography variant="caption" color="text.secondary" noWrap>
                              {result.studentCode}
                            </Typography>
                          </Box>
                        </Box>

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
          <NavIconButton active={location.pathname === '/'} onClick={handleHomeClick}>
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

              {/* Messenger */}
              <Box sx={{ position: 'relative' }} ref={msgRef}>
                <Tooltip title="Tin nhắn">
                  <ActionIconButton onClick={() => setShowMsgDropdown(!showMsgDropdown)}>
                    <Badge badgeContent={unreadMessageCount} color="error" max={9}>
                      <ChatBubbleIcon />
                    </Badge>
                  </ActionIconButton>
                </Tooltip>

                {showMsgDropdown && (
                  <MessengerDropdown
                    onClose={() => setShowMsgDropdown(false)}
                    onMessageRead={handleMessageRead}
                  />
                )}
              </Box>

              {/* Notification */}
              <Box ref={notiRef}>
                <NotificationBell />
              </Box>

              <Tooltip title="Cài đặt">
                <ActionIconButton
                  data-testid="header-settings-button"
                  onClick={handleSettingsClick}
                  aria-controls={openSettings ? 'settings-menu' : undefined}
                  aria-haspopup="true"
                  aria-expanded={openSettings ? 'true' : undefined}
                  aria-label="Cài đặt"
                >
                  <SettingsIcon />
                </ActionIconButton>
              </Tooltip>

              <Tooltip title={liveUser?.fullName || user?.fullName || 'Tài khoản'}>
                <Box
                  onClick={() => navigateToProfile()}
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1,
                    cursor: 'pointer',
                    ml: 1,
                    p: '4px 8px',
                    borderRadius: '20px',
                    '&:hover': { bgcolor: '#F2F2F2' }
                  }}
                >
                  <AvatarWithFrame
                    src={liveUser?.avatarUrl || user?.avatarUrl}
                    name={liveUser?.fullName || user?.fullName}
                    frameClass={
                      (liveUser as any)?.currentAvatarFrame ||
                      (user as any)?.currentAvatarFrame
                    }
                    size={28}
                  />

                  <Typography
                    variant="body2"
                    sx={{
                      fontWeight: 600,
                      display: { xs: 'none', lg: 'block' }
                    }}
                  >
                    <ColoredName
                      name={(liveUser?.fullName || user?.fullName || '')
                        .split(' ')
                        .pop() || ''}
                      colorClass={
                        (liveUser as any)?.currentNameColor ||
                        (user as any)?.currentNameColor
                      }
                    />
                  </Typography>
                </Box>
              </Tooltip>

              {/* 🟢 SETTINGS MENU (Đã được hợp nhất gọn gàng) */}
              <Menu
                id="settings-menu"
                anchorEl={settingsAnchorEl}
                open={openSettings}
                onClose={handleSettingsClose}
                PaperProps={{
                  elevation: 0,
                  sx: {
                    overflow: 'visible',
                    filter: 'drop-shadow(0px 2px 8px rgba(0,0,0,0.15))',
                    mt: 1.5,
                    width: 280,
                    borderRadius: 2,
                  },
                }}
                transformOrigin={{ horizontal: 'right', vertical: 'top' }}
                anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
              >
                <MenuItem onClick={() => { navigate('/settings/security'); handleSettingsClose(); }}>
                  <ListItemIcon>
                    <SecurityIcon fontSize="small" />
                  </ListItemIcon>
                  Lịch sử đăng nhập
                </MenuItem>

                {/* Tích hợp Đổi mật khẩu vào chung Menu */}
                <MenuItem onClick={handleOpenChangePassword}>
                  <ListItemIcon>
                    <LockResetIcon fontSize="small" />
                  </ListItemIcon>
                  Đổi mật khẩu
                </MenuItem>

                <MenuItem onClick={() => { toggleColorMode(); handleSettingsClose(); }}>
                  <ListItemIcon><DarkModeIcon fontSize="small" /></ListItemIcon>
                  {mode === 'light' ? 'Giao diện tối' : 'Giao diện sáng'}
                </MenuItem>

                <Divider />

                <MenuItem
                  data-testid="header-logout"
                  onClick={handleLogout}
                  sx={{ color: 'error.main' }}
                >
                  <ListItemIcon>
                    <LogoutIcon fontSize="small" color="error" />
                  </ListItemIcon>
                  Đăng xuất
                </MenuItem>
              </Menu>

              {/* Modal đổi mật khẩu */}
              {openChangePassword && <ChangePasswordModal onClose={() => setOpenChangePassword(false)} />}
            </>
          ) : (
            <Button
              variant="contained"
              color="primary"
              onClick={() => navigate('/login')}
            >
              Đăng nhập
            </Button>
          )}
        </Box>
      </Toolbar>
    </AppBar>
  );
}