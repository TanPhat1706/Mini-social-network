import React, { useState } from 'react';
import {
  Box, Paper, Avatar, Button, Divider, IconButton,
  Dialog, DialogTitle, DialogContent, DialogActions, TextField, Typography,
  Tooltip
} from '@mui/material';
import { styled } from '@mui/material/styles';

// Import Icons
import PhotoLibraryIcon from '@mui/icons-material/PhotoLibrary'; // Icon Ảnh
import SentimentSatisfiedAltIcon from '@mui/icons-material/SentimentSatisfiedAlt'; // Icon Cảm xúc
import CloseIcon from '@mui/icons-material/Close';

// Mock data cho người dùng đang đăng nhập
const currentUser = {
  name: "Lê Hông Phát",
  avatarUrl: "https://placehold.co/40x40/EFEFEF/333?text=LHP",
};

// --- Styled Component cho ô input giả ---
const FakeInputButton = styled(Button)(({ theme }) => ({
  flexGrow: 1,
  borderRadius: '20px',
  backgroundColor: theme.palette.background.default, // Màu nền xám nhạt
  color: theme.palette.text.secondary,
  justifyContent: 'flex-start',
  padding: '8px 16px',
  textTransform: 'none',
  '&:hover': {
    backgroundColor: theme.palette.action.hover,
  },
}));

// ------------------------------------------

export default function CreatePost() {
  // State để quản lý việc Bật/Tắt Modal
  const [open, setOpen] = useState(false);
  // State để lưu nội dung bài đăng
  const [postContent, setPostContent] = useState('');

  const handleClickOpen = () => {
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
    setPostContent(''); // Xóa nội dung khi đóng
  };

  const handlePost = () => {
    // TODO: Xử lý logic đăng bài (gọi API)
    console.log('Đăng bài:', postContent);
    handleClose(); // Đóng modal sau khi đăng
  };

  return (
    <>
      {/* PHẦN 1: KHUNG TẠO BÀI VIẾT (NHƯ ẢNH 1)
        Đây là component 'Paper' đã được style
      */}
      <Paper 
        elevation={0} 
        sx={{ 
          border: '1px solid #E0E0E0', 
          p: 2, 
          mb: 3,
          cursor: 'pointer',
          '&:hover': { bgcolor: 'action.hover' }
        }}
        onClick={handleClickOpen} // Click vào đâu cũng mở modal
      >
        {/* Hàng trên: Avatar và Input giả */}
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 1.5 }}>
          <Avatar src={currentUser.avatarUrl} alt={currentUser.name} sx={{ mr: 1.5 }} />
          <FakeInputButton fullWidth>
            Phát ơi, bạn đang nghĩ gì thế?
          </FakeInputButton>
        </Box>
        
        <Divider />
        
        {/* Hàng dưới: 2 Icon (Ảnh, Cảm xúc) */}
        <Box sx={{ display: 'flex', justifyContent: 'space-around', pt: 1 }}>
          <Button startIcon={<PhotoLibraryIcon sx={{ color: '#45bd62' }} />} sx={{ color: 'text.secondary' }}>
            Ảnh
          </Button>
          <Button startIcon={<SentimentSatisfiedAltIcon sx={{ color: '#f7b928' }} />} sx={{ color: 'text.secondary' }}>
            Cảm xúc
          </Button>
        </Box>
      </Paper>


      {/* PHẦN 2: MODAL TẠO BÀI VIẾT (POPUP NHƯ ẢNH 2)
      */}
      <Dialog open={open} onClose={handleClose} fullWidth maxWidth="sm">
        {/* Tiêu đề Modal */}
        <DialogTitle>
          <Typography variant="h6" component="span" sx={{ fontWeight: 'bold', textAlign: 'center', display: 'block' }}>
            Tạo bài viết
          </Typography>
          <IconButton
            aria-label="close"
            onClick={handleClose}
            sx={{
              position: 'absolute',
              right: 8,
              top: 8,
              color: (theme) => theme.palette.grey[500],
            }}
          >
            <CloseIcon />
          </IconButton>
        </DialogTitle>
        
        <Divider />
        
        {/* Nội dung Modal */}
        <DialogContent>
          {/* Thông tin người dùng */}
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
            <Avatar src={currentUser.avatarUrl} alt={currentUser.name} />
            <Box sx={{ ml: 1.5 }}>
              <Typography variant="body1" sx={{ fontWeight: 'bold' }}>
                {currentUser.name}
              </Typography>
              {/* Đã loại bỏ phần "Công khai" theo yêu cầu */}
            </Box>
          </Box>
          
          {/* Khung nhập text */}
          <TextField
            autoFocus
            fullWidth
            multiline
            rows={5}
            variant="standard" // Dùng 'standard' để không có viền
            InputProps={{ disableUnderline: true }} // Bỏ gạch chân
            placeholder="Phát ơi, bạn đang nghĩ gì thế?"
            value={postContent}
            onChange={(e) => setPostContent(e.target.value)}
            sx={{ fontSize: '1.5rem' }} // Chữ to
          />
          
          {/* Phần "Thêm vào bài viết" (đã giản lược) */}
          <Paper 
            variant="outlined" 
            sx={{ mt: 2, p: 1, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
          >
            <Typography variant="body2" sx={{ ml: 1, fontWeight: 500 }}>
              Thêm vào bài viết
            </Typography>
            <Box>
              <Tooltip title="Ảnh">
                <IconButton sx={{ color: '#45bd62' }}>
                  <PhotoLibraryIcon />
                </IconButton>
              </Tooltip>
              <Tooltip title="Cảm xúc">
                <IconButton sx={{ color: '#f7b928' }}>
                  <SentimentSatisfiedAltIcon />
                </IconButton>
              </Tooltip>
            </Box>
          </Paper>
        </DialogContent>
        
        {/* Nút Đăng */}
        <DialogActions sx={{ p: 2 }}>
          <Button 
            fullWidth 
            variant="contained" 
            onClick={handlePost}
            disabled={!postContent.trim()} // Vô hiệu hóa nút khi chưa nhập gì
          >
            Đăng
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}