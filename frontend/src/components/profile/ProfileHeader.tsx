import React, { useState, useRef, useEffect } from 'react';
import {
  Box, Container, Typography, Button, IconButton,
  Menu, MenuItem, Divider, Tabs, Tab, Paper, ListItemIcon
} from '@mui/material';

// Import Icons
import CameraAltIcon from '@mui/icons-material/CameraAlt';
import EditIcon from '@mui/icons-material/Edit';
import PersonIcon from '@mui/icons-material/Person';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import DeleteIcon from '@mui/icons-material/Delete';

// Import API & Components
import api from '../../api/api';
import { getApiBaseUrl } from '../../config/apiBase';
import { useAuth } from '../../context/AuthContext';
import { showError, showInfo, showSuccess } from '../../utils/swal';
import PostViewModal from './PostViewModal';
import EditProfileDialog from './EditProfileDialog';
import FriendButton from '../friend/FriendButton';
import AvatarWithFrame from '../AvatarWithFrame'; 
import ColoredName from '../ColoredName'; 
import type { User } from '../../types';
import { useChat } from '../../context/ChatContext';
import ImageCropperModal from './ImageCropperModal';

// 🟢 IMPORT HÀM TÍNH THỜI GIAN
import { differenceInMinutes, differenceInHours, differenceInDays } from 'date-fns';

const getImageUrl = (url: string | undefined) => {
  if (!url) return undefined;
  const timestamp = new Date().getTime();
  if (url.startsWith('http')) {
    return url.includes('?') ? `${url}&t=${timestamp}` : `${url}?t=${timestamp}`;
  }
  return `${getApiBaseUrl()}${url}?t=${timestamp}`;
};

interface ProfileHeaderProps {
  profileUser: User | null;
  isSelfProfile: boolean;
  onUpdateProfile: (user: User) => void;
  friendCount?: number;
  activeTab: number; 
  onTabChange: (event: React.SyntheticEvent, newValue: number) => void; 
}

// 🟢 INTERFACE TRẠNG THÁI (PRESENCE)
interface UserPresence {
  online: boolean;
  lastSeen?: string;
}

// 🟢 HÀM XỬ LÝ TEXT TRẠNG THÁI HOẠT ĐỘNG (Dùng cho Trang cá nhân)
const formatPresenceText = (presence: UserPresence | null) => {
  if (!presence) return '';
  if (presence.online) return 'Đang hoạt động';
  if (!presence.lastSeen) return '';

  const lastDate = new Date(presence.lastSeen);
  const mins = differenceInMinutes(new Date(), lastDate);

  if (mins < 60) return `Hoạt động ${mins <= 0 ? 1 : mins} phút trước`;

  const hours = differenceInHours(new Date(), lastDate);
  if (hours < 24) return `Hoạt động ${hours} giờ trước`;

  const days = differenceInDays(new Date(), lastDate);
  if (days < 7) return `Hoạt động ${days} ngày trước`;

  return ''; // Ẩn đi nếu lâu quá
};

export default function ProfileHeader({ profileUser, isSelfProfile, onUpdateProfile, friendCount = 0, activeTab, onTabChange }: ProfileHeaderProps) {
  const { user: currentUser } = useAuth();
  const { openChat } = useChat();
  const [avatarMenuAnchor, setAvatarMenuAnchor] = useState<null | HTMLElement>(null);
  const [coverMenuAnchor, setCoverMenuAnchor] = useState<null | HTMLElement>(null);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [postViewOpen, setPostViewOpen] = useState(false);
  const [isUploading, setIsUploading] = useState(false);

  // 🟢 STATE LƯU TRẠNG THÁI HOẠT ĐỘNG
  const [presence, setPresence] = useState<UserPresence | null>(null);

  const avatarInputRef = useRef<HTMLInputElement>(null);
  const coverInputRef = useRef<HTMLInputElement>(null);

  const [cropModalOpen, setCropModalOpen] = useState(false);
  const [cropImageSrc, setCropImageSrc] = useState<string | null>(null);
  const [cropType, setCropType] = useState<'avatar' | 'cover'>('avatar');

  // 🟢 EFFECT: GỌI API LẤY TRẠNG THÁI HOẠT ĐỘNG
  useEffect(() => {
    // Không cần lấy trạng thái nếu đang ở trang của chính mình
    if (!profileUser?.studentCode || isSelfProfile) return;

    const fetchPresence = async () => {
      try {
        const res = await api.get(`/api/users/${profileUser.studentCode}/presence`);
        setPresence(res.data);
      } catch (error) {
        console.error("Lỗi lấy trạng thái hoạt động trang cá nhân", error);
      }
    };

    fetchPresence(); // Gọi ngay lần đầu
    const interval = setInterval(fetchPresence, 60000); // Lặp lại mỗi 60s

    return () => clearInterval(interval);
  }, [profileUser?.studentCode, isSelfProfile]);

  const closeMenus = () => { setAvatarMenuAnchor(null); setCoverMenuAnchor(null); };

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>, type: 'avatar' | 'cover') => {
    const file = event.target.files?.[0];
    if (!file || !profileUser) return;

    const reader = new FileReader();
    reader.onload = () => {
      setCropImageSrc(reader.result as string);
      setCropType(type);
      setCropModalOpen(true); 
      closeMenus();
    };
    reader.readAsDataURL(file);
    if (event.target) event.target.value = ''; 
  };

  const handleCropComplete = async (croppedFile: File) => {
    setCropModalOpen(false);
    setIsUploading(true);
    try {
      const formData = new FormData();
      formData.append(cropType, croppedFile); 
      const res = await api.put('/api/auth/profile', formData, {
        headers: { 'Content-Type': undefined },
      });

      if (typeof res.data === 'string' || !res.data.avatarUrl) {
        const newAvatarUrl = res.data.avatarUrl || res.data;
        onUpdateProfile({ ...profileUser, avatarUrl: newAvatarUrl } as User);
      } else {
        onUpdateProfile(res.data);
      }
      showSuccess("Cập nhật ảnh thành công!");
    } catch (error) {
      showError("Không thể tải ảnh lên.");
    } finally {
      setIsUploading(false);
    }
  };

  const handleRemoveCover = async () => {
    closeMenus(); 
    setIsUploading(true);
    try {
      const res = await api.delete('/api/auth/profile/cover');
      onUpdateProfile(res.data);
      showSuccess("Đã gỡ ảnh bìa thành công!");
    } catch (error) {
      showError("Không thể gỡ ảnh bìa lúc này.");
    } finally {
      setIsUploading(false);
    }
  };

  if (!profileUser) return null;
  
  // 🟢 TÍNH TOÁN TEXT VÀ MÀU SẮC TRẠNG THÁI
  const presenceText = formatPresenceText(presence);
  // Nếu đang online thì màu xanh lá, nếu offline nhưng dưới 60 phút thì màu xanh dương nhạt (giống sidebar), còn lại màu xám
  const isOnline = presence?.online;
  const isRecentOffline = !isOnline && presenceText.includes('phút');

  return (
    <>
      {isSelfProfile && (
        <>
          <input type="file" hidden accept="image/*" ref={avatarInputRef} onChange={(e) => handleFileSelect(e, 'avatar')} />
          <input type="file" hidden accept="image/*" ref={coverInputRef} onChange={(e) => handleFileSelect(e, 'cover')} />
        </>
      )}

      <Paper elevation={1} sx={{ bgcolor: 'background.paper', mb: 3, borderRadius: { xs: 0, md: '0 0 8px 8px' } }}>
        <Container maxWidth="lg" sx={{ px: { xs: 0, sm: 2, md: 4 } }}>

          {/* COVER PHOTO */}
          <Box
            sx={{
              height: { xs: 200, md: 350 },
              background: profileUser.coverPhotoUrl ? `url(${getImageUrl(profileUser.coverPhotoUrl)}) center/cover` : 'linear-gradient(to right, #00c6ff, #0072ff)',
              position: 'relative',
              borderRadius: { xs: 0, md: '0 0 8px 8px' }
            }}
          >
            {isSelfProfile && (
              <Button
                variant="contained" startIcon={<CameraAltIcon />} onClick={(e) => setCoverMenuAnchor(e.currentTarget)}
                sx={{ 
                  position: 'absolute', 
                  bottom: 16, 
                  right: 16, 
                  bgcolor: 'background.paper', 
                  color: 'text.primary', 
                  fontWeight: 'bold', 
                  '&:hover': { bgcolor: 'action.hover' }, 
                  textTransform: 'none',
                  zIndex: 10 
                }}
              >
                {isUploading ? 'Đang tải...' : 'Thêm ảnh bìa'}
              </Button>
            )}
          </Box>

          {/* AVATAR & INFO */}
          <Box sx={{ display: 'flex', flexDirection: { xs: 'column', md: 'row' }, alignItems: { xs: 'center', md: 'flex-end' }, mt: { xs: -8, md: -6 }, position: 'relative', px: { xs: 2, md: 4 } }}>
            <Box onClick={isSelfProfile ? (e) => setAvatarMenuAnchor(e.currentTarget) : undefined} sx={{ p: 0.5, bgcolor: 'background.paper', borderRadius: '50%', cursor: isSelfProfile ? 'pointer' : 'default', position: 'relative' }}>
              <AvatarWithFrame src={getImageUrl(profileUser.avatarUrl) || `https://ui-avatars.com/api/?name=${profileUser.fullName}`} frameClass={(profileUser as any).currentAvatarFrame} size={168} />
              
              {/* 🟢 RENDER CHẤM XANH NỔI LÊN MẶT AVATAR NẾU ONLINE (CHỈ DÀNH CHO KHÁCH XEM) */}
              {!isSelfProfile && isOnline && (
                <Box sx={{
                  position: 'absolute', bottom: 18, right: 18, width: 22, height: 22,
                  backgroundColor: '#31a24c', borderRadius: '50%',
                  border: '4px solid', borderColor: 'background.paper', zIndex: 10
                }} />
              )}

              {isSelfProfile && (
                <IconButton sx={{ position: 'absolute', bottom: 8, right: 8, bgcolor: 'background.default', border: '1px solid', borderColor: 'divider', '&:hover': { bgcolor: 'action.hover' } }}>
                  <CameraAltIcon />
                </IconButton>
              )}
            </Box>

            <Box sx={{ mt: 2, ml: { md: 3 }, textAlign: { xs: 'center', md: 'left' }, flex: 1, pb: 2 }}>
              <Typography variant="h3" fontWeight="bold"><ColoredName name={profileUser.fullName} colorClass={(profileUser as any).currentNameColor} /></Typography>
              <Typography variant="subtitle1" color="text.secondary" sx={{ fontWeight: 500, mt: 0.5 }}>
                {friendCount} người bạn
              </Typography>
              
              {/* 🟢 RENDER DÒNG CHỮ TRẠNG THÁI HOẠT ĐỘNG (NGAY DƯỚI SỐ BẠN BÈ) */}
              {!isSelfProfile && presenceText && (
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: { xs: 'center', md: 'flex-start' }, mt: 0.5, gap: 0.8 }}>
                  <Typography 
                    variant="body2" 
                    sx={{ 
                      fontWeight: isOnline || isRecentOffline ? 600 : 500, 
                      color: isOnline ? '#31a24c' : (isRecentOffline ? '#1877f2' : 'text.secondary') 
                    }}
                  >
                    {presenceText}
                  </Typography>
                </Box>
              )}
            </Box>

            <Box sx={{ display: 'flex', gap: 1, pb: 2, mt: { xs: 2, md: 0 } }}>
              {isSelfProfile ? (
                <Button variant="contained" startIcon={<EditIcon />} onClick={() => setEditDialogOpen(true)} sx={{ fontWeight: 'bold', textTransform: 'none' }}>
                  Chỉnh sửa trang cá nhân
                </Button>
              ) : (
                <>
                  <Box sx={{ width: 140 }}><FriendButton targetUserId={profileUser.id} currentUserId={currentUser?.id ?? 0} /></Box>
                  <Button
                    variant="outlined"
                    onClick={() => openChat(profileUser)}
                    sx={{ fontWeight: 'bold', textTransform: 'none' }}
                  >
                    Nhắn tin
                  </Button>
                </>
              )}
            </Box>
          </Box>

          <Divider />
          <Tabs value={activeTab} onChange={onTabChange} sx={{ px: { xs: 2, md: 4 } }}>
            <Tab label="Bài viết" sx={{ textTransform: 'none', fontWeight: 600 }} />
            <Tab label="Giới thiệu" sx={{ textTransform: 'none', fontWeight: 600 }} />
            <Tab label="Bạn bè" sx={{ textTransform: 'none', fontWeight: 600 }} />
          </Tabs>
        </Container>
      </Paper>

      {/* CÁC MODAL VÀ MENU GIỮ NGUYÊN BÊN DƯỚI */}
      <Menu anchorEl={avatarMenuAnchor} open={Boolean(avatarMenuAnchor)} onClose={closeMenus}>
        <MenuItem onClick={() => { setPostViewOpen(true); closeMenus(); }}>
          <ListItemIcon><PersonIcon /></ListItemIcon> Xem ảnh đại diện
        </MenuItem>
        <MenuItem onClick={() => { avatarInputRef.current?.click(); closeMenus(); }}>
          <ListItemIcon><UploadFileIcon /></ListItemIcon> Chọn ảnh đại diện
        </MenuItem>
      </Menu>

      <Menu anchorEl={coverMenuAnchor} open={Boolean(coverMenuAnchor)} onClose={closeMenus}>
        <MenuItem onClick={() => { coverInputRef.current?.click(); closeMenus(); }}>
          <ListItemIcon><UploadFileIcon /></ListItemIcon> Tải ảnh lên
        </MenuItem>
        
        {profileUser.coverPhotoUrl && (
          <MenuItem onClick={handleRemoveCover} sx={{ color: 'error.main' }}>
            <ListItemIcon><DeleteIcon color="error" /></ListItemIcon> Gỡ
          </MenuItem>
        )}
      </Menu>

      <EditProfileDialog open={editDialogOpen} onClose={() => setEditDialogOpen(false)} currentUser={profileUser} onUpdateSuccess={(updated) => { onUpdateProfile(updated); setEditDialogOpen(false); }} />
      <PostViewModal
        open={postViewOpen}
        onClose={() => setPostViewOpen(false)}
        imageUrl={profileUser.avatarUrl} 
        frameClass={(profileUser as any).currentAvatarFrame} 
      />
      <ImageCropperModal
        open={cropModalOpen}
        imageSrc={cropImageSrc}
        aspectRatio={cropType === 'avatar' ? 1 : 16 / 9} 
        onClose={() => setCropModalOpen(false)}
        onCropDone={handleCropComplete}
      />
    </>
  );
}