import React, { useState } from 'react';
import {
  Box, Container, Avatar, Typography, Button, IconButton,
  Menu, MenuItem, Divider, Tabs, Tab, Paper
} from '@mui/material';
import { styled } from '@mui/material/styles';

// Import Icons
import CameraAltIcon from '@mui/icons-material/CameraAlt'; // Icon camera
import EditIcon from '@mui/icons-material/Edit'; // Icon chỉnh sửa
import MoreHorizIcon from '@mui/icons-material/MoreHoriz';
import PhotoCameraBackIcon from '@mui/icons-material/PhotoCameraBack';
import PersonIcon from '@mui/icons-material/Person';
import ImageIcon from '@mui/icons-material/Image'; // Icon "Chọn ảnh bìa"
import UploadFileIcon from '@mui/icons-material/UploadFile'; // Icon "Tải ảnh lên"
import DeleteIcon from '@mui/icons-material/Delete'; // Icon "Gỡ"


// Import Modal
import PostViewModal from './PostViewModal';

// Mock data cho trang cá nhân
const mockUser = {
  name: "Lê Hồng Phát", // Tên từ ảnh
  friendCount: 275,
  avatarUrl: "https://placehold.co/168x168/EFEFEF/333?text=AVATAR",
  coverUrl: "https://placehold.co/1000x350/CCCCCC/333?text=COVER+IMAGE",
};

// --- Styled Components ---
// Box chứa ảnh bìa
const CoverPhoto = styled(Box)(({ theme }) => ({
  height: '350px',
  backgroundSize: 'cover',
  backgroundPosition: 'center',
  borderRadius: '0 0 8px 8px',
  cursor: 'pointer',
  position: 'relative',
}));

// Avatar (nằm đè lên ảnh bìa)
const ProfileAvatar = styled(Avatar)(({ theme }) => ({
  width: 168,
  height: 168,
  border: `4px solid ${theme.palette.background.paper}`,
  position: 'absolute',
  bottom: -32,
  left: 32,
}));
// -------------------------

export default function ProfileHeader() {
  // State cho Menu của Avatar
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const menuOpen = Boolean(anchorEl);
  // State cho Menu của Background
  const [coverMenuAnchorEl, setCoverMenuAnchorEl] = useState<null | HTMLElement>(null);
  const coverMenuOpen = Boolean(coverMenuAnchorEl);


  // State cho Modal xem bài đăng
  const [modalOpen, setModalOpen] = useState(false);
  // TODO: Thêm state để biết xem post nào (avatar hay cover)

  // State cho Tab Navbar
  const [currentTab, setCurrentTab] = useState(0);

  const handleAvatarClick = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };
  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleCoverMenuClick = (event: React.MouseEvent<HTMLElement>) => {
    setCoverMenuAnchorEl(event.currentTarget);
  };
  const handleCoverMenuClose = () => {
    setCoverMenuAnchorEl(null);
  };

  // Mở Modal (dùng chung cho cả avatar và cover)
  const handleViewPost = () => {
    setModalOpen(true);
    handleMenuClose();
  };

  const handleCloseModal = () => {
    setModalOpen(false);
  };

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setCurrentTab(newValue);
  };

  return (
    <>
      <Paper 
        elevation={0} 
        sx={{ 
          bgcolor: 'background.paper', 
          mb: 3, 
          border: '1px solid #E0E0E0',
          borderTop: 'none', // Header chính đã có viền
        }}
      >
        {/* 1. ẢNH BÌA VÀ AVATAR */}
        <Container maxWidth="md">
          <Box sx={{ position: 'relative' }}>
            {/* 1a. Ảnh bìa */}
            <CoverPhoto
              sx={{ backgroundImage: `url(${mockUser.coverUrl})` }}
            >
              <Button
                variant="contained"
                startIcon={<CameraAltIcon />}
                sx={{
                  position: 'absolute', bottom: 16, right: 16,
                  bgcolor: 'rgba(255,255,255,0.9)',
                  color: 'black',
                  '&:hover': { bgcolor: 'white' }
                }}
                onClick={handleCoverMenuClick}
              >
                Chỉnh sửa ảnh bìa
              </Button>
            </CoverPhoto>

            {/* 1b. Avatar */}
            <ProfileAvatar
              src={mockUser.avatarUrl}
              alt={mockUser.name}
              onClick={handleAvatarClick} // Click Avatar -> Mở Menu
              sx={{ cursor: 'pointer' }}
            />
            
            {/* Nút camera trên Avatar */}
            <IconButton
              onClick={handleAvatarClick}
              sx={{
                position: 'absolute',
                bottom: -20,
                left: 170,
                bgcolor: 'action.hover',
                '&:hover': { bgcolor: 'action.hover' }
              }}
            >
              <CameraAltIcon />
            </IconButton>
          </Box>

          {/* 2. TÊN, BẠN BÈ, NÚT BẤM */}
          <Box sx={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-end',
            pt: 2, // Kéo lên
            pb: 2,
            pl: '220px', // Bắt đầu sau Avatar
            pr: 2,
            mt: '-16px' // Giảm khoảng cách
          }}>
            {/* Tên và số bạn */}
            <Box>
              <Typography variant="h4" sx={{ fontWeight: 'bold' }}>
                {mockUser.name}
              </Typography>
              <Typography variant="body1" color="text.secondary">
                {mockUser.friendCount} bạn bè
              </Typography>
            </Box>

            {/* Các nút (Đã loại bỏ "Thêm vào tin") */}
            <Box>
              <Button variant="contained" startIcon={<EditIcon />} sx={{ mr: 1 }}>
                Chỉnh sửa trang cá nhân
              </Button>
              <Button variant="contained" sx={{ bgcolor: 'action.hover', color: 'text.primary', minWidth: 0, p: 1 }}>
                <MoreHorizIcon />
              </Button>
            </Box>
          </Box>

          <Divider />

          {/* 3. THANH NAVBAR (Đã giản lược) */}
          <Box sx={{ pl: '204px' }}>
            <Tabs value={currentTab} onChange={handleTabChange}>
              <Tab label="Bài viết" />
              <Tab label="Giới thiệu" />
              <Tab label="Bạn bè" />
            </Tabs>
          </Box>
        </Container>
      </Paper>

      {/* 4. MENU KHI ẤN VÀO AVATAR (Dropdown) */}
      <Menu
        anchorEl={anchorEl}
        open={menuOpen}
        onClose={handleMenuClose}
      >
        {/* Yêu cầu của bạn: "Xem ảnh đại diện" -> Mở Modal */}
        <MenuItem onClick={handleViewPost}>
          <PersonIcon sx={{ mr: 1.5 }} />
          Xem ảnh đại diện
        </MenuItem>
        
        {/* Yêu cầu của bạn: "Chọn ảnh đại diện" -> Không làm gì */}
        <MenuItem onClick={handleMenuClose}>
          <PhotoCameraBackIcon sx={{ mr: 1.5 }} />
          Chọn ảnh đại diện
        </MenuItem>
      </Menu>

      {/* ⭐️ THÊM MENU MỚI KHI ẤN VÀO "CHỈNH SỬA ẢNH BÌA" */}
      <Menu
        anchorEl={coverMenuAnchorEl}
        open={coverMenuOpen}
        onClose={handleCoverMenuClose}
      >
        {/* Theo yêu cầu: 'Chọn ảnh bìa' */}
        <MenuItem onClick={handleCoverMenuClose}>
          <ImageIcon sx={{ mr: 1.5 }} />
          Chọn ảnh bìa
        </MenuItem>
        
        {/* Theo yêu cầu: 'Tải ảnh lên' */}
        <MenuItem onClick={handleCoverMenuClose}>
          <UploadFileIcon sx={{ mr: 1.5 }} />
          Tải ảnh lên
        </MenuItem>
        <Divider />
        
        {/* Theo yêu cầu: 'Gỡ' */}
        <MenuItem onClick={handleCoverMenuClose} sx={{ color: 'error.main' }}>
          <DeleteIcon sx={{ mr: 1.5 }} />
          Gỡ
        </MenuItem>
      </Menu>

      {/* 5. MODAL (POPUP) ĐỂ XEM BÀI ĐĂNG */}
      <PostViewModal open={modalOpen} onClose={handleCloseModal} />
    </>
  );
}