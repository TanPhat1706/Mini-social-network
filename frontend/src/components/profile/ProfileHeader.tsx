import React, { useState, useRef } from 'react';
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
import { showError, showInfo } from '../../utils/swal';
import PostViewModal from './PostViewModal';
import EditProfileDialog from './EditProfileDialog';
import FriendButton from '../friend/FriendButton';
import AvatarWithFrame from '../AvatarWithFrame'; // Fix đường dẫn
import ColoredName from '../ColoredName'; // Fix đường dẫn
import type { User } from '../../types';
import { useChat } from '../../context/ChatContext';

const getImageUrl = (url: string | undefined) => {
  if (!url) return undefined;
  if (url.startsWith('http')) return url;
  return `${getApiBaseUrl()}${url}`;
};

interface ProfileHeaderProps {
  profileUser: User | null;
  isSelfProfile: boolean;
  onUpdateProfile: (user: User) => void;
}

export default function ProfileHeader({ profileUser, isSelfProfile, onUpdateProfile }: ProfileHeaderProps) {
  const { user: currentUser } = useAuth();
  const { openChat } = useChat();
  const [avatarMenuAnchor, setAvatarMenuAnchor] = useState<null | HTMLElement>(null);
  const [coverMenuAnchor, setCoverMenuAnchor] = useState<null | HTMLElement>(null);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [postViewOpen, setPostViewOpen] = useState(false);
  const [isUploading, setIsUploading] = useState(false);

  const avatarInputRef = useRef<HTMLInputElement>(null);
  const coverInputRef = useRef<HTMLInputElement>(null);

  const closeMenus = () => { setAvatarMenuAnchor(null); setCoverMenuAnchor(null); };

  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>, type: 'avatar' | 'cover') => {
    const file = event.target.files?.[0];
    if (!file) return;
    setIsUploading(true);
    closeMenus();
    try {
      const formData = new FormData();
      formData.append(type, file);
      const res = await api.put('/api/auth/profile', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      onUpdateProfile(res.data);
    } catch (error) {
      showError("Không thể tải ảnh lên.");
    } finally {
      setIsUploading(false);
      if (event.target) event.target.value = '';
    }
  };

  if (!profileUser) return null;

  return (
    <>
      {isSelfProfile && (
        <>
          <input type="file" hidden accept="image/*" ref={avatarInputRef} onChange={(e) => handleFileUpload(e, 'avatar')} />
          <input type="file" hidden accept="image/*" ref={coverInputRef} onChange={(e) => handleFileUpload(e, 'cover')} />
        </>
      )}

      {/* 🔴 ĐỔI BÓNG ĐỔ VÀ NỀN CHUẨN MUI */}
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
                sx={{ position: 'absolute', bottom: 16, right: 16, bgcolor: 'background.paper', color: 'text.primary', fontWeight: 'bold', '&:hover': { bgcolor: 'action.hover' }, textTransform: 'none' }}
              >
                {isUploading ? 'Đang tải...' : 'Thêm ảnh bìa'}
              </Button>
            )}
          </Box>

          {/* AVATAR & INFO */}
          <Box sx={{ display: 'flex', flexDirection: { xs: 'column', md: 'row' }, alignItems: { xs: 'center', md: 'flex-end' }, mt: { xs: -8, md: -6 }, position: 'relative', px: { xs: 2, md: 4 } }}>
            {/* 🔴 BỌC NỀN AVATAR BẰNG MÀU CHUẨN MUI */}
            <Box onClick={isSelfProfile ? (e) => setAvatarMenuAnchor(e.currentTarget) : undefined} sx={{ p: 0.5, bgcolor: 'background.paper', borderRadius: '50%', cursor: isSelfProfile ? 'pointer' : 'default', position: 'relative' }}>
              <AvatarWithFrame src={getImageUrl(profileUser.avatarUrl) || `https://ui-avatars.com/api/?name=${profileUser.fullName}`} frameClass={(profileUser as any).currentAvatarFrame} size={168} />
              {isSelfProfile && (
                <IconButton sx={{ position: 'absolute', bottom: 8, right: 8, bgcolor: 'background.default', border: '1px solid', borderColor: 'divider', '&:hover': { bgcolor: 'action.hover' } }}>
                  <CameraAltIcon />
                </IconButton>
              )}
            </Box>

            <Box sx={{ mt: 2, ml: { md: 3 }, textAlign: { xs: 'center', md: 'left' }, flex: 1, pb: 2 }}>
              <Typography variant="h3" fontWeight="bold"><ColoredName name={profileUser.fullName} colorClass={(profileUser as any).currentNameColor} /></Typography>
              <Typography variant="body1" color="text.secondary" sx={{ mt: 0.5, fontWeight: 500 }}>0 người bạn</Typography>
            </Box>

            {/* BUTTONS */}
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
          <Tabs value={0} sx={{ px: { xs: 2, md: 4 } }}>
            <Tab label="Bài viết" sx={{ textTransform: 'none', fontWeight: 600 }} />
            <Tab label="Giới thiệu" sx={{ textTransform: 'none', fontWeight: 600 }} />
            <Tab label="Bạn bè" sx={{ textTransform: 'none', fontWeight: 600 }} />
          </Tabs>
        </Container>
      </Paper>

      <Menu anchorEl={avatarMenuAnchor} open={Boolean(avatarMenuAnchor)} onClose={closeMenus}>
        <MenuItem onClick={() => { setPostViewOpen(true); closeMenus(); }}>
          <ListItemIcon><PersonIcon /></ListItemIcon> Xem ảnh đại diện
        </MenuItem>
        <MenuItem onClick={() => { avatarInputRef.current?.click(); closeMenus(); }}>
          <ListItemIcon><UploadFileIcon /></ListItemIcon> Chọn ảnh đại diện
        </MenuItem>
      </Menu>

      <Menu anchorEl={coverMenuAnchor} open={Boolean(coverMenuAnchor)} onClose={closeMenus}>
        <MenuItem onClick={() => { coverInputRef.current?.click(); closeMenus(); }}><ListItemIcon><UploadFileIcon /></ListItemIcon> Tải ảnh lên</MenuItem>
        <MenuItem onClick={() => { showInfo('Đang phát triển'); closeMenus(); }} sx={{ color: 'error.main' }}><ListItemIcon><DeleteIcon color="error" /></ListItemIcon> Gỡ</MenuItem>
      </Menu>

      <EditProfileDialog open={editDialogOpen} onClose={() => setEditDialogOpen(false)} currentUser={profileUser} onUpdateSuccess={(updated) => { onUpdateProfile(updated); setEditDialogOpen(false); }} />
      <PostViewModal
        open={postViewOpen}
        onClose={() => setPostViewOpen(false)}
        imageUrl={profileUser.avatarUrl} // Truyền url ảnh thật vào
        frameClass={(profileUser as any).currentAvatarFrame} // Truyền viền vào
      />
    </>
  );
}