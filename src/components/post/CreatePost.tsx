import React, { useState, useRef, useEffect } from 'react'; // ⭐️ Thêm useEffect
import {
  Box, Paper, Avatar, Button, Divider, IconButton,
  Dialog, DialogTitle, DialogContent, DialogActions, TextField, Typography,
  Tooltip, CircularProgress, ImageList, ImageListItem, Skeleton
} from '@mui/material';
import { styled } from '@mui/material/styles';

import PhotoLibraryIcon from '@mui/icons-material/PhotoLibrary';
import SentimentSatisfiedAltIcon from '@mui/icons-material/SentimentSatisfiedAlt';
import CloseIcon from '@mui/icons-material/Close';
import api from '../../api/api';

// --- Styled Components ---
const FakeInputButton = styled(Button)(({ theme }) => ({
  flexGrow: 1,
  borderRadius: '20px',
  backgroundColor: theme.palette.background.default,
  color: theme.palette.text.secondary,
  justifyContent: 'flex-start',
  padding: '8px 16px',
  textTransform: 'none',
  '&:hover': {
    backgroundColor: theme.palette.action.hover,
  },
}));

export default function CreatePost() {
  // ⭐️ State cho User Info
  const [user, setUser] = useState(null); 
  
  const [open, setOpen] = useState(false);
  const [postContent, setPostContent] = useState('');
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [previewUrls, setPreviewUrls] = useState([]);
  const [isLoading, setIsLoading] = useState(false);

  const fileInputRef = useRef(null);

  // ⭐️ FETCH USER PROFILE TẠI ĐÂY
  useEffect(() => {
    const fetchUserProfile = async () => {
        try {
            const response = await api.get('/api/auth/profile');
            setUser(response.data);
        } catch (err) {
            console.error("Failed to fetch user profile in CreatePost:", err);
            // Có thể set user mặc định nếu lỗi
        }
    };
    fetchUserProfile();
  }, []);

  const handleClickOpen = () => setOpen(true);

  const handleClose = () => {
    if (isLoading) return;
    setOpen(false);
    resetForm();
  };

  const resetForm = () => {
    setPostContent('');
    setSelectedFiles([]);
    previewUrls.forEach(url => URL.revokeObjectURL(url));
    setPreviewUrls([]);
  };

  const handleFileSelect = (e) => {
    const files = Array.from(e.target.files);
    if (files.length === 0) return;
    setSelectedFiles(prev => [...prev, ...files]);
    const newPreviewUrls = files.map(file => URL.createObjectURL(file));
    setPreviewUrls(prev => [...prev, ...newPreviewUrls]);
  };

  const handleRemoveImage = (index) => {
    setSelectedFiles(prev => prev.filter((_, i) => i !== index));
    URL.revokeObjectURL(previewUrls[index]);
    setPreviewUrls(prev => prev.filter((_, i) => i !== index));
  };

  const handlePost = async () => {
    if (!postContent.trim() && selectedFiles.length === 0) return;
    setIsLoading(true);
    try {
      const formData = new FormData();
      formData.append('content', postContent);
      formData.append('visibility', 'PUBLIC');
      selectedFiles.forEach((file) => {
        formData.append('mediaFiles', file);
      });

      await api.post('/api/posts', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      
      // ⭐️ Reload lại trang hoặc callback ra ngoài để refresh list (tùy logic của bạn)
      window.location.reload(); // Tạm thời reload để thấy post mới
      handleClose();
    } catch (error) {
      console.error('Error:', error);
      alert('Lỗi kết nối server!');
    } finally {
      setIsLoading(false);
    }
  };

  const onClickPickImage = () => {
    fileInputRef.current.click();
  };

  // ⭐️ Lấy tên user để hiển thị (Fallback nếu user chưa load xong)
  const userName = user ? user.fullName : 'Bạn';
  const userAvatar = user ? user.avatarUrl : '';

  return (
    <>
      <input
        type="file"
        multiple
        accept="image/*,video/*"
        ref={fileInputRef}
        style={{ display: 'none' }}
        onChange={handleFileSelect}
      />

      {/* PHẦN 1: TRIGGER BUTTON */}
      <Paper 
        elevation={0} 
        sx={{ border: '1px solid #E0E0E0', p: 2, mb: 3, cursor: 'pointer', '&:hover': { bgcolor: 'action.hover' }}}
        onClick={handleClickOpen}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 1.5 }}>
          {/* ⭐️ Hiển thị Skeleton nếu chưa load xong user */}
          {user ? (
             <Avatar src={userAvatar} alt={userName} sx={{ mr: 1.5 }} />
          ) : (
             <Skeleton variant="circular" width={40} height={40} sx={{ mr: 1.5 }} />
          )}
          
          <FakeInputButton fullWidth>
            {user ? `${userName} ơi, bạn đang nghĩ gì thế?` : 'Đang tải...'}
          </FakeInputButton>
        </Box>
        <Divider />
        <Box sx={{ display: 'flex', justifyContent: 'space-around', pt: 1 }}>
          <Button startIcon={<PhotoLibraryIcon sx={{ color: '#45bd62' }} />} sx={{ color: 'text.secondary' }}>
            Ảnh
          </Button>
          <Button startIcon={<SentimentSatisfiedAltIcon sx={{ color: '#f7b928' }} />} sx={{ color: 'text.secondary' }}>
            Cảm xúc
          </Button>
        </Box>
      </Paper>

      {/* PHẦN 2: MODAL */}
      <Dialog open={open} onClose={handleClose} fullWidth maxWidth="sm">
        <DialogTitle>
          <Typography variant="h6" component="span" sx={{ fontWeight: 'bold', textAlign: 'center', display: 'block' }}>
            Tạo bài viết
          </Typography>
          <IconButton
            onClick={handleClose}
            disabled={isLoading}
            sx={{ position: 'absolute', right: 8, top: 8, color: (theme) => theme.palette.grey[500] }}
          >
            <CloseIcon />
          </IconButton>
        </DialogTitle>
        
        <Divider />
        
        <DialogContent>
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
            <Avatar src={userAvatar} alt={userName} />
            <Box sx={{ ml: 1.5 }}>
              <Typography variant="body1" sx={{ fontWeight: 'bold' }}>{userName}</Typography>
              <Typography variant="caption" color="text.secondary">Công khai</Typography>
            </Box>
          </Box>
          
          <TextField
            autoFocus
            fullWidth
            multiline
            rows={3}
            variant="standard"
            InputProps={{ disableUnderline: true }}
            placeholder={`${userName} ơi, bạn đang nghĩ gì thế?`}
            value={postContent}
            onChange={(e) => setPostContent(e.target.value)}
            sx={{ fontSize: '1.2rem', mb: 2 }}
          />

          {previewUrls.length > 0 && (
            <Paper variant="outlined" sx={{ p: 1, mb: 2, maxHeight: 200, overflowY: 'auto' }}>
                <ImageList cols={3} rowHeight={100} gap={8}>
                    {previewUrls.map((url, index) => (
                        <ImageListItem key={index}>
                            <img src={url} alt={`Preview ${index}`} loading="lazy" style={{ height: '100px', objectFit: 'cover', borderRadius: 4 }} />
                            <IconButton 
                                size="small"
                                sx={{ position: 'absolute', top: 0, right: 0, bgcolor: 'rgba(0,0,0,0.5)', color: 'white' }}
                                onClick={() => handleRemoveImage(index)}
                            >
                                <CloseIcon fontSize="small"/>
                            </IconButton>
                        </ImageListItem>
                    ))}
                </ImageList>
            </Paper>
          )}

          <Paper variant="outlined" sx={{ mt: 2, p: 1, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="body2" sx={{ ml: 1, fontWeight: 500 }}>Thêm vào bài viết</Typography>
            <Box>
              <Tooltip title="Ảnh/Video">
                <IconButton sx={{ color: '#45bd62' }} onClick={onClickPickImage}>
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
        
        <DialogActions sx={{ p: 2 }}>
          <Button 
            fullWidth 
            variant="contained" 
            onClick={handlePost}
            disabled={(!postContent.trim() && selectedFiles.length === 0) || isLoading}
          >
            {isLoading ? <CircularProgress size={24} color="inherit"/> : "Đăng"}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}