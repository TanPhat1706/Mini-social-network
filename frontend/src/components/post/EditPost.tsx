import React, { useState } from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Button, IconButton, Box, Typography,
  ImageList, ImageListItem, CircularProgress
} from '@mui/material';
import { showError } from '../../utils/swal';
import CloseIcon from '@mui/icons-material/Close';
import PhotoLibraryIcon from '@mui/icons-material/PhotoLibrary';
import { getApiBaseUrl } from '../../config/apiBase';

export default function EditPost({ open, onClose, post, onUpdateSuccess }) {
  const [content, setContent] = useState(post.content || '');
  const [newFiles, setNewFiles] = useState([]); // File mới chọn
  const [previewUrls, setPreviewUrls] = useState([]); // Preview file mới
  const [isLoading, setIsLoading] = useState(false);

  // Xử lý chọn ảnh mới
  const handleFileSelect = (e) => {
    const files = Array.from(e.target.files);
    if (files.length === 0) return;

    setNewFiles(files); // Lưu file để upload
    
    // Tạo preview
    const urls = files.map(file => URL.createObjectURL(file));
    setPreviewUrls(urls);
  };

  const handleSave = async () => {
    setIsLoading(true);
    try {
      const formData = new FormData();
      formData.append('content', content);
      formData.append('visibility', post.visibility || 'PUBLIC');
      
      // Chỉ append file nếu có file mới
      newFiles.forEach(file => formData.append('mediaFiles', file));

      const token = localStorage.getItem('token');
      const response = await fetch(`${getApiBaseUrl()}/api/posts/${post.id}`, {
        method: 'PUT',
        headers: token ? { Authorization: `Bearer ${token}`, 'ngrok-skip-browser-warning': 'true' } : { 'ngrok-skip-browser-warning': 'true' },
        body: formData,
      });

      if (response.ok) {
        const updatedPost = await response.json();
        onUpdateSuccess(updatedPost); // Báo ra ngoài để cập nhật UI
        onClose();
      } else {
        showError('Cập nhật thất bại!');
      }
    } catch (error) {
      console.error(error);
      showError('Lỗi server!');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle sx={{ m: 0, p: 2 }}>
        <Typography variant="h6">Chỉnh sửa bài viết</Typography>
        <IconButton onClick={onClose} sx={{ position: 'absolute', right: 8, top: 8 }}>
          <CloseIcon />
        </IconButton>
      </DialogTitle>
      
      <DialogContent dividers>
        <TextField
          fullWidth multiline rows={4} variant="standard"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          InputProps={{ disableUnderline: true }}
          placeholder="Nội dung bài viết..."
        />

        {/* HIỂN THỊ ẢNH */}
        <Box mt={2}>
           {/* Trường hợp 1: Đang chọn ảnh mới -> Hiển thị Preview mới */}
           {previewUrls.length > 0 ? (
              <Box>
                <Typography variant="caption" color="primary">Ảnh mới sẽ thay thế ảnh cũ:</Typography>
                <ImageList cols={3} rowHeight={100}>
                  {previewUrls.map((url, idx) => (
                    <ImageListItem key={idx}>
                      <img src={url} alt="new-preview" style={{ height: 100, objectFit: 'cover' }} />
                    </ImageListItem>
                  ))}
                </ImageList>
              </Box>
           ) : (
             /* Trường hợp 2: Chưa chọn ảnh mới -> Hiển thị ảnh cũ (nếu có) */
             post.mediaUrls && post.mediaUrls.length > 0 && (
               <Box>
                 <Typography variant="caption" color="text.secondary">Ảnh hiện tại:</Typography>
                 <ImageList cols={3} rowHeight={100}>
                   {post.mediaUrls.map((url, idx) => (
                     <ImageListItem key={idx}>
                       <img src={url} alt="old-media" style={{ height: 100, objectFit: 'cover', opacity: 0.7 }} />
                     </ImageListItem>
                   ))}
                 </ImageList>
               </Box>
             )
           )}
        </Box>

        {/* Nút chọn ảnh thay thế */}
        <Box mt={2}>
          <input
            id={`edit-file-input-${post.id}`} // ID duy nhất
            type="file" multiple hidden accept="image/*"
            onChange={handleFileSelect}
          />
          <label htmlFor={`edit-file-input-${post.id}`}>
            <Button component="span" startIcon={<PhotoLibraryIcon />} size="small">
              Thay đổi ảnh
            </Button>
          </label>
        </Box>

      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} color="inherit">Hủy</Button>
        <Button onClick={handleSave} variant="contained" disabled={isLoading}>
          {isLoading ? <CircularProgress size={24} /> : "Lưu thay đổi"}
        </Button>
      </DialogActions>
    </Dialog>
  );
}