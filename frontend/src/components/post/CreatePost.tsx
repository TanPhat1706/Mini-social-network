import React, { useState, useRef, useEffect } from 'react';
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
import { showError } from '../../utils/swal';

import AvatarWithFrame from '../AvatarWithFrame';
import ColoredName from '../ColoredName';

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
  // 🟢 FIX LỖI 3 & 4: Định nghĩa rõ kiểu dữ liệu là <any> để TypeScript không báo lỗi 'never'
  const [user, setUser] = useState<any>(null);

  const [open, setOpen] = useState(false);
  const [postContent, setPostContent] = useState('');
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [previewUrls, setPreviewUrls] = useState<{ url: string, type: string }[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  
  // 🟢 FIX LỖI 2: Định nghĩa rõ đây là Ref của một thẻ Input HTML
  const fileInputRef = useRef<HTMLInputElement>(null);
  const MAX_FILE_SIZE = Number(import.meta.env.VITE_APP_MAX_FILE_SIZE) || 104857600; // Fallback 100MB

  useEffect(() => {
    const fetchUserProfile = async () => {
      try {
        const response = await api.get('/api/auth/profile');
        setUser(response.data);
      } catch (err) {
        console.error("Failed to fetch user profile in CreatePost:", err);
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
    // 🟢 FIX LỖI 1: Truy cập đúng thuộc tính .url của object
    previewUrls.forEach(item => URL.revokeObjectURL(item.url));
    setPreviewUrls([]);
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files) return;
    const files = Array.from(e.target.files);
    if (files.length === 0) return;

    const validFiles: File[] = [];
    const newPreviewUrls: { url: string, type: string }[] = [];
    let hasOversizedFile = false;

    files.forEach(file => {
      if (file.size > MAX_FILE_SIZE) {
        hasOversizedFile = true;
      } else {
        validFiles.push(file);
        newPreviewUrls.push({
          url: URL.createObjectURL(file),
          type: file.type
        });
      }
    });

    if (hasOversizedFile) {
      import('sweetalert2').then((Swal) => {
        Swal.default.fire({
          icon: 'error',
          title: 'File quá lớn!',
          text: 'Một hoặc nhiều file vượt quá giới hạn và đã bị loại bỏ.',
          customClass: {
            container: 'swal-z-index-fix'
          }
        });
      });
    }

    setSelectedFiles(prev => [...prev, ...validFiles]);
    setPreviewUrls(prev => [...prev, ...newPreviewUrls]);

    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleRemoveImage = (index: number) => {
    setSelectedFiles(prev => prev.filter((_, i) => i !== index));
    URL.revokeObjectURL(previewUrls[index].url);
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
        headers: { 'Content-Type': undefined },
        timeout: 60000, 
      });

      handleClose();
    } catch (error) {
      console.error('Create post error:', error);
      const serverMessage =
        (error as any)?.response?.data?.message ||
        (error as any)?.response?.data ||
        'Lỗi kết nối server!';
      showError(String(serverMessage));
    } finally {
      setIsLoading(false);
    }
  };

  const onClickPickImage = () => {
    // 🟢 FIX LỖI 2: Dùng optional chaining (?) để an toàn 100%
    fileInputRef.current?.click();
  };

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
        data-testid="create-post-trigger"
        elevation={0}
        sx={{ border: '1px solid #E0E0E0', p: 2, mb: 3, cursor: 'pointer', '&:hover': { bgcolor: 'action.hover' } }}
        onClick={handleClickOpen}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 1.5 }}>
          {user ? (
            <Box sx={{ mr: 1.5 }}>
              <AvatarWithFrame
                src={userAvatar}
                name={userName}
                frameClass={user?.currentAvatarFrame}
                size={40}
              />
            </Box>
          ) : (
            <Skeleton variant="circular" width={40} height={40} sx={{ mr: 1.5 }} />
          )}

          <FakeInputButton fullWidth>
            {user ? (
              <><ColoredName name={userName} colorClass={user?.currentNameColor} />‎ ơi, bạn đang nghĩ gì thế?</>
            ) : 'Đang tải...'}
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
            <AvatarWithFrame
              src={userAvatar}
              name={userName}
              frameClass={user?.currentAvatarFrame}
              size={40}
            />
            <Box sx={{ ml: 1.5 }}>
              <Typography variant="body1" sx={{ fontWeight: 'bold' }}>
                <ColoredName name={userName} colorClass={user?.currentNameColor} />
              </Typography>
              <Typography variant="caption" color="text.secondary">Công khai</Typography>
            </Box>
          </Box>

          <TextField
            data-testid="create-post-content"
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
                {previewUrls.map((fileData, index) => (
                  <ImageListItem key={index} sx={{ position: 'relative' }}>
                    {fileData.type.startsWith('video/') ? (
                      <video
                        src={fileData.url}
                        muted
                        autoPlay
                        loop
                        style={{ height: '100px', width: '100%', objectFit: 'cover', borderRadius: 4 }}
                      />
                    ) : (
                      <img
                        src={fileData.url}
                        alt={`Preview ${index}`}
                        loading="lazy"
                        style={{ height: '100px', width: '100%', objectFit: 'cover', borderRadius: 4 }}
                      />
                    )}

                    <IconButton
                      size="small"
                      sx={{ position: 'absolute', top: 4, right: 4, bgcolor: 'rgba(0,0,0,0.6)', color: 'white', '&:hover': { bgcolor: 'rgba(0,0,0,0.8)' } }}
                      onClick={() => handleRemoveImage(index)}
                    >
                      <CloseIcon fontSize="small" />
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
            data-testid="create-post-submit"
            fullWidth
            variant="contained"
            onClick={handlePost}
            disabled={(!postContent.trim() && selectedFiles.length === 0) || isLoading}
          >
            {isLoading ? <CircularProgress size={24} color="inherit" /> : "Đăng"}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}