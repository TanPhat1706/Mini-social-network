import React, { useState, useEffect } from 'react';
import { 
  Dialog, DialogTitle, DialogContent, DialogActions, 
  Button, TextField, Box, IconButton, Typography, CircularProgress 
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import api from '../../api/api'; // Hoặc axiosClient
import { showError } from '../../utils/swal';
import type { User } from '../../types';

interface Props {
  open: boolean;
  onClose: () => void;
  currentUser: User;
  onUpdateSuccess: (updatedUser: User) => void;
}

export default function EditProfileDialog({ open, onClose, currentUser, onUpdateSuccess }: Props) {
  const [fullName, setFullName] = useState('');
  const [bio, setBio] = useState('');
  const [className, setClassName] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // Load dữ liệu cũ khi mở form
  useEffect(() => {
    if (currentUser) {
      setFullName(currentUser.fullName || '');
      setBio(currentUser.bio || '');
      setClassName(currentUser.className || '');
    }
  }, [currentUser, open]);

  const handleSave = async () => {
    setIsLoading(true);
    try {
      const formData = new FormData();
      formData.append('fullName', fullName);
      formData.append('bio', bio);
      formData.append('className', className);

      // Gọi API PUT /api/auth/profile mà ta đã xây dựng ở Backend
      const res = await api.put('/api/auth/profile', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      
      onUpdateSuccess(res.data);
      onClose();
    } catch (error) {
      console.error("Lỗi cập nhật:", error);
      showError("Cập nhật thất bại!");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle sx={{ m: 0, p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h6" fontWeight="bold">Chỉnh sửa trang cá nhân</Typography>
        <IconButton onClick={onClose}><CloseIcon /></IconButton>
      </DialogTitle>
      
      <DialogContent dividers>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3, py: 1 }}>
          <TextField
            label="Họ và tên"
            fullWidth
            variant="outlined"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
          />
          <TextField
            label="Tiểu sử"
            fullWidth
            multiline
            rows={3}
            variant="outlined"
            placeholder="Mô tả ngắn về bản thân..."
            value={bio}
            onChange={(e) => setBio(e.target.value)}
          />
          <TextField
            label="Lớp / Khoa"
            fullWidth
            variant="outlined"
            placeholder="Ví dụ: KTPM K14"
            value={className}
            onChange={(e) => setClassName(e.target.value)}
          />
        </Box>
      </DialogContent>

      <DialogActions sx={{ p: 2 }}>
        <Button onClick={onClose} sx={{ color: '#65676B' }}>Hủy</Button>
        <Button variant="contained" onClick={handleSave} disabled={isLoading}>
          {isLoading ? <CircularProgress size={24} color="inherit" /> : "Lưu thay đổi"}
        </Button>
      </DialogActions>
    </Dialog>
  );
}