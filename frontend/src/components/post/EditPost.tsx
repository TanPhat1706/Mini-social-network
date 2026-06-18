import React, { useState } from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Button, IconButton, Box, Typography,
  ImageList, ImageListItem, CircularProgress,
  Tooltip, Select, MenuItem, FormControl
} from '@mui/material';
import { showError } from '../../utils/swal';
import CloseIcon from '@mui/icons-material/Close';
import PhotoLibraryIcon from '@mui/icons-material/PhotoLibrary';
import { getApiBaseUrl } from '../../config/apiBase';
import api from '../../api/api';
import type { PostData, PostMedia } from './CardPost';
import PublicIcon from '@mui/icons-material/Public';
import LockIcon from '@mui/icons-material/Lock';
import PendingIcon from '@mui/icons-material/Pending';
import GroupIcon from '@mui/icons-material/Group';

interface EditPostProps {
  open: boolean;
  onClose: () => void;
  post: PostData;
  onUpdateSuccess: (updatedPost: PostData) => void;
}

export default function EditPost({ open, onClose, post, onUpdateSuccess }: EditPostProps) {
  const [content, setContent] = useState(post.content || '');
  const [retainedMedia, setRetainedMedia] = useState<PostMedia[]>(post.media || []);
  const [newFiles, setNewFiles] = useState<File[]>([]);
  const [newPreviewUrls, setNewPreviewUrls] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [visibility, setVisibility] = useState(post.visibility || 'PUBLIC');

  // Xử lý chọn ảnh mới
  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []);
    if (files.length === 0) return;

    setNewFiles(prev => [...prev, ...files]);

    // Tạo preview
    const urls = files.map(file => URL.createObjectURL(file));
    setNewPreviewUrls(prev => [...prev, ...urls]);
  };

  // Xóa ảnh cũ khỏi danh sách giữ lại
  const handleRemoveOldMedia = (mediaIdToRemove: number) => {
    setRetainedMedia(prev => prev.filter(media => media.id !== mediaIdToRemove));
  }

  // Xóa ảnh mới khỏi danh sách chuẩn bị upload
  const handleRemoveNewMedia = (indexToRemove: number) => {
    setNewFiles(prev => prev.filter((_, idx) => idx !== indexToRemove));

    // Revoke Object URL để tránh Memory Leak trên trình duyệt
    URL.revokeObjectURL(newPreviewUrls[indexToRemove]);
    setNewPreviewUrls(prev => prev.filter((_, idx) => idx !== indexToRemove));
  }

  const handleSave = async () => {
    setIsLoading(true);
    try {
      const formData = new FormData();
      formData.append('content', content);
      formData.append('visibility', visibility);

      retainedMedia.forEach(media => {
        formData.append('retainedMediaIds', media.id.toString());
      });

      // Chỉ append file nếu có file mới
      newFiles.forEach(file => formData.append('mediaFiles', file));

      const response = await api.put(`/api/posts/${post.id}`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 60000,
      });

      onUpdateSuccess(response.data);
      onClose();
    } catch (error) {
      console.error(error);
      showError('Cập nhật bài viết thất bại. Vui lòng thử lại.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle sx={{ m: 0, p: 2 }}>
        <Typography variant="h6" component="div" fontWeight="bold">Chỉnh sửa bài viết</Typography>
        <IconButton onClick={onClose} disabled={isLoading} sx={{ position: 'absolute', right: 8, top: 8 }}>
          <CloseIcon />
        </IconButton>
      </DialogTitle>

      <DialogContent dividers>
        <Box sx={{ mb: 2 }}>
          <FormControl size="small" sx={{ minWidth: 160 }}>
            <Select
              value={visibility}
              onChange={(e) => setVisibility(e.target.value)}
              sx={{
                borderRadius: 2,
                backgroundColor: 'action.hover',
                '& .MuiSelect-select': { display: 'flex', alignItems: 'center', py: 0.8, fontSize: '0.875rem', fontWeight: 500 }
              }}
            >
              <MenuItem value="PUBLIC">
                <PublicIcon fontSize="small" sx={{ mr: 1, color: 'text.secondary' }} /> Công khai
              </MenuItem>
              <MenuItem value="CLASS">
                <GroupIcon fontSize="small" sx={{ mr: 1, color: 'text.secondary' }} /> Lớp học
              </MenuItem>
              <MenuItem value="PRIVATE">
                <LockIcon fontSize="small" sx={{ mr: 1, color: 'text.secondary' }} /> Chỉ mình tôi
              </MenuItem>

              {post.visibility === 'PENDING' && (
                <MenuItem value="PENDING" disabled>
                  <PendingIcon fontSize="small" sx={{ mr: 1, color: 'warning.main' }} /> Đang chờ duyệt
                </MenuItem>
              )}
            </Select>
          </FormControl>
        </Box>

        <TextField
          fullWidth multiline minRows={3} variant="standard"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          InputProps={{ disableUnderline: true }}
          placeholder="Nội dung bài viết..."
          sx={{ fontSize: '1.1rem', mb: 2 }}
        />

        <Box>
          {/* HIỂN THỊ ẢNH CŨ CÒN GIỮ LẠI */}
          {retainedMedia.length > 0 && (
            <Box mb={2}>
              <Typography variant="caption" color="text.secondary" fontWeight="bold">Ảnh đã tải lên:</Typography>
              <ImageList cols={3} rowHeight={100} gap={8} sx={{ mt: 1 }}>
                {retainedMedia.map((media) => (
                  <ImageListItem key={media.id} sx={{ position: 'relative' }}>
                    <img src={media.url.startsWith('http') ? media.url : `${getApiBaseUrl()}${media.url}`} alt="old-media" style={{ height: 100, objectFit: 'cover', borderRadius: 4 }} />
                    <IconButton size="small" onClick={() => handleRemoveOldMedia(media.id)} sx={{ position: 'absolute', top: 4, right: 4, bgcolor: 'rgba(0,0,0,0.6)', color: 'white', '&:hover': { bgcolor: 'rgba(0,0,0,0.8)' } }}>
                      <CloseIcon fontSize="small" />
                    </IconButton>
                  </ImageListItem>
                ))}
              </ImageList>
            </Box>
          )}

          {/* HIỂN THỊ ẢNH MỚI CHỌN THÊM */}
          {newPreviewUrls.length > 0 && (
            <Box mb={2}>
              <Typography variant="caption" color="primary" fontWeight="bold">Ảnh mới thêm:</Typography>
              <ImageList cols={3} rowHeight={100} gap={8} sx={{ mt: 1 }}>
                {newPreviewUrls.map((url, idx) => (
                  <ImageListItem key={idx} sx={{ position: 'relative' }}>
                    <img src={url} alt="new-preview" style={{ height: 100, objectFit: 'cover', borderRadius: 4 }} />
                    <IconButton size="small" onClick={() => handleRemoveNewMedia(idx)} sx={{ position: 'absolute', top: 4, right: 4, bgcolor: 'rgba(0,0,0,0.6)', color: 'white', '&:hover': { bgcolor: 'rgba(0,0,0,0.8)' } }}>
                      <CloseIcon fontSize="small" />
                    </IconButton>
                  </ImageListItem>
                ))}
              </ImageList>
            </Box>
          )}
        </Box>

        <Box mt={2} display="flex" alignItems="center" gap={1}>
          <input id={`edit-file-input-${post.id}`} type="file" multiple hidden accept="image/*,video/*" onChange={handleFileSelect} />
          <label htmlFor={`edit-file-input-${post.id}`}>
            <Tooltip title="Thêm ảnh/video">
              <IconButton component="span" sx={{ color: '#45bd62', bgcolor: 'action.hover' }}>
                <PhotoLibraryIcon />
              </IconButton>
            </Tooltip>
          </label>
        </Box>

      </DialogContent>
      <DialogActions sx={{ p: 2 }}>
        <Button onClick={onClose} color="inherit" disabled={isLoading}>Hủy</Button>
        <Button onClick={handleSave} variant="contained" disabled={isLoading}>
          {isLoading ? <CircularProgress size={24} color="inherit" /> : "Lưu thay đổi"}
        </Button>
      </DialogActions>
    </Dialog>
  );
}