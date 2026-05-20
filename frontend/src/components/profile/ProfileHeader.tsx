import React, { useState, useRef, useEffect } from 'react';
import {
  Box, Container, Avatar, Typography, Button, IconButton,
  Menu, MenuItem, Divider, Tabs, Tab, Paper, ListItemIcon, CircularProgress
} from '@mui/material';
import { styled } from '@mui/material/styles';

// Import Icons
import CameraAltIcon from '@mui/icons-material/CameraAlt';
import EditIcon from '@mui/icons-material/Edit';
import MoreHorizIcon from '@mui/icons-material/MoreHoriz';
import ImageIcon from '@mui/icons-material/Image';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import DeleteIcon from '@mui/icons-material/Delete';
import PersonIcon from '@mui/icons-material/Person';

// Import API & Components
import api from '../../api/api';
import { getApiBaseUrl } from '../../config/apiBase';
import { useAuth } from '../../context/AuthContext';
import { showError, showInfo } from '../../utils/swal';
import PostViewModal from './PostViewModal';
import EditProfileDialog from './EditProfileDialog'; // Import Dialog mới
import FriendButton from '../friend/FriendButton';

// --- Styled Components ---
const CoverPhoto = styled(Box)(({ theme }) => ({
  height: '350px',
  backgroundSize: 'cover',
  backgroundPosition: 'center',
  borderRadius: '0 0 8px 8px',
  cursor: 'pointer',
  position: 'relative',
  backgroundColor: '#f0f2f5', // Màu nền backup nếu chưa có ảnh
}));

const ProfileAvatar = styled(Avatar)(({ theme }) => ({
  width: 168,
  height: 168,
  border: `4px solid ${theme.palette.background.paper}`,
  position: 'absolute',
  bottom: -32,
  left: 32,
}));

// Helper xử lý URL ảnh thủ công (Local)
const getImageUrl = (url: string | undefined) => {
  if (!url) return undefined;
  if (url.startsWith('http')) return url;
  return `${getApiBaseUrl()}${url}`;
};

interface ProfileHeaderProps {
  profileUser: User | null;
  isSelfProfile: boolean;
}

export default function ProfileHeader({ profileUser: profileUserProp, isSelfProfile }: ProfileHeaderProps) {
  const { user: currentUser } = useAuth();
  const [profileUser, setProfileUser] = useState<User | null>(profileUserProp);

  // Refresh data khi props profileUser thay đổi
  useEffect(() => { setProfileUser(profileUserProp); }, [profileUserProp]);

  // State Menu
  const [avatarMenuAnchor, setAvatarMenuAnchor] = useState<null | HTMLElement>(null);
  const [coverMenuAnchor, setCoverMenuAnchor] = useState<null | HTMLElement>(null);
  
  // State Modals
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [postViewOpen, setPostViewOpen] = useState(false);
  const [isUploading, setIsUploading] = useState(false);

  // Refs input file ẩn
  const avatarInputRef = useRef<HTMLInputElement>(null);
  const coverInputRef = useRef<HTMLInputElement>(null);

  // --- HANDLERS MENU ---
  const handleAvatarClick = (e: React.MouseEvent<HTMLElement>) => setAvatarMenuAnchor(e.currentTarget);
  const handleCoverClick = (e: React.MouseEvent<HTMLElement>) => setCoverMenuAnchor(e.currentTarget);
  const closeMenus = () => { setAvatarMenuAnchor(null); setCoverMenuAnchor(null); };

  // --- LOGIC UPLOAD ẢNH (QUAN TRỌNG) ---
  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>, type: 'avatar' | 'cover') => {
    const file = event.target.files?.[0];
    if (!file) return;

    setIsUploading(true);
    closeMenus();

    try {
      const formData = new FormData();
      // Backend của chúng ta nhận 'avatar' hoặc 'cover'
      formData.append(type, file); 

      const res = await api.put('/api/auth/profile', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      
      // Cập nhật UI ngay lập tức
      setProfileUser(res.data);
      // Nếu muốn cập nhật global user context: login(res.data.token) (tuỳ logic auth của bạn)
      
    } catch (error) {
      console.error("Upload lỗi:", error);
      showError("Không thể tải ảnh lên. Vui lòng thử lại.");
    } finally {
      setIsUploading(false);
      // Reset input để chọn lại file cũ được
      if (event.target) event.target.value = '';
    }
  };

  const handleRemovePhoto = async (type: 'avatar' | 'cover') => {
     // TODO: Gọi API xóa ảnh (Cần backend hỗ trợ set null). 
     // Tạm thời chưa triển khai backend nên để placeholder
     showInfo("Chức năng đang phát triển ở Backend");
     closeMenus();
  };

  if (!profileUser) return null;

  return (
    <>
      {/* INPUT ẨN ĐỂ CHỌN FILE */}
      {isSelfProfile && (
        <>
          <input 
            type="file" hidden accept="image/*" 
            ref={avatarInputRef} 
            onChange={(e) => handleFileUpload(e, 'avatar')} 
          />
          <input 
            type="file" hidden accept="image/*" 
            ref={coverInputRef} 
            onChange={(e) => handleFileUpload(e, 'cover')} 
          />
        </>
      )}

      <Paper elevation={0} sx={{ bgcolor: 'background.paper', mb: 3, border: '1px solid #E0E0E0', borderTop: 'none' }}>
        <Container maxWidth="md">
          <Box sx={{ position: 'relative' }}>
            {/* 1. ẢNH BÌA */}
            <CoverPhoto sx={{ backgroundImage: `url(${getImageUrl(profileUser.coverPhotoUrl)})` }}>
              {isSelfProfile && (
                <Button
                  variant="contained"
                  startIcon={<CameraAltIcon />}
                  onClick={handleCoverClick}
                  sx={{
                    position: 'absolute', bottom: 16, right: 16,
                    bgcolor: 'white', color: 'black', fontWeight: 'bold',
                    '&:hover': { bgcolor: '#f2f2f2' }, textTransform: 'none'
                  }}
                >
                  {isUploading ? 'Đang tải...' : 'Thêm ảnh bìa'}
                </Button>
              )}
            </CoverPhoto>

            {/* 2. AVATAR */}
            <Box
              onClick={isSelfProfile ? handleAvatarClick : undefined}
              sx={{ cursor: isSelfProfile ? 'pointer' : 'default' }}
            >
               <ProfileAvatar src={getImageUrl(profileUser.avatarUrl)} alt={profileUser.fullName} />
               {isSelfProfile && (
                 <IconButton
                   sx={{
                     position: 'absolute', bottom: -20, left: 160,
                     bgcolor: '#E4E6E9', p: 1,
                     '&:hover': { bgcolor: '#D8DADF' }
                   }}
                 >
                   <CameraAltIcon sx={{ color: 'black' }} />
                 </IconButton>
               )}
            </Box>
          </Box>

          {/* 3. THÔNG TIN & NÚT EDIT */}
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', pt: 2, pb: 2, pl: '220px', pr: 2, mt: '-16px' }}>
            <Box>
              <Typography variant="h4" sx={{ fontWeight: 'bold' }}>{profileUser.fullName}</Typography>
              <Typography variant="body1" color="text.secondary">
                 {/* Logic đếm bạn bè cần API riêng, tạm để mockup hoặc lấy từ user */}
                 0 bạn bè 
              </Typography>
            </Box>

            <Box sx={{ display: 'flex', gap: 1 }}>
              {isSelfProfile ? (
                <Button 
                  variant="contained" 
                  startIcon={<EditIcon />} 
                  onClick={() => setEditDialogOpen(true)}
                  sx={{ bgcolor: '#1877F2', fontWeight: 'bold', textTransform: 'none' }}
                >
                  Chỉnh sửa trang cá nhân
                </Button>
              ) : (
                <>
                  <Box sx={{ width: 140 }}> 
                    <FriendButton
                      targetUserId={profileUser.id}
                      currentUserId={currentUser?.id ?? 0}
                    />
                  </Box>
                  <Button
                    variant="outlined"
                    onClick={() => showInfo('Chức năng nhắn tin đang phát triển.')}
                    sx={{ fontWeight: 'bold', textTransform: 'none' }}
                  >
                    Nhắn tin
                  </Button>
                </>
              )}
            </Box>
          </Box>

          <Divider />
          
          {/* TABS */}
          <Box sx={{ pl: '204px' }}>
            <Tabs value={0}>
              <Tab label="Bài viết" sx={{ textTransform: 'none', fontWeight: 600 }} />
              <Tab label="Giới thiệu" sx={{ textTransform: 'none', fontWeight: 600 }} />
              <Tab label="Bạn bè" sx={{ textTransform: 'none', fontWeight: 600 }} />
            </Tabs>
          </Box>
        </Container>
      </Paper>

      {/* --- MENUS --- */}
      
      {/* MENU AVATAR */}
      <Menu anchorEl={avatarMenuAnchor} open={Boolean(avatarMenuAnchor)} onClose={closeMenus}>
        <MenuItem onClick={() => { setPostViewOpen(true); closeMenus(); }}>
          <ListItemIcon><PersonIcon /></ListItemIcon> Xem ảnh đại diện
        </MenuItem>
        <MenuItem onClick={() => avatarInputRef.current?.click()}>
          <ListItemIcon><UploadFileIcon /></ListItemIcon> Chọn ảnh đại diện
        </MenuItem>
      </Menu>

      {/* MENU COVER */}
      <Menu anchorEl={coverMenuAnchor} open={Boolean(coverMenuAnchor)} onClose={closeMenus}>
        <MenuItem onClick={() => coverInputRef.current?.click()}>
          <ListItemIcon><UploadFileIcon /></ListItemIcon> Tải ảnh lên
        </MenuItem>
        <MenuItem onClick={() => handleRemovePhoto('cover')} sx={{ color: 'error.main' }}>
          <ListItemIcon><DeleteIcon color="error" /></ListItemIcon> Gỡ
        </MenuItem>
      </Menu>

      {/* --- DIALOGS --- */}
      <EditProfileDialog 
        open={editDialogOpen} 
        onClose={() => setEditDialogOpen(false)} 
        currentUser={profileUser} 
        onUpdateSuccess={(updated) => setProfileUser(updated)}
      />
      
      <PostViewModal 
        open={postViewOpen} 
        onClose={() => setPostViewOpen(false)} 
        // post={...} // Truyền ảnh avatar vào đây nếu muốn xem
      />
    </>
  );
}