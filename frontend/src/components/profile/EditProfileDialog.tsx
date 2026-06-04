import React, { useState, useEffect, useRef } from 'react';
import { 
  Dialog, DialogTitle, DialogContent, DialogActions, 
  Button, TextField, Box, IconButton, Typography, CircularProgress, Divider
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import PhotoCameraIcon from '@mui/icons-material/PhotoCamera';
import api from '../../api/api';
import { showError, showSuccess } from '../../utils/swal';
import type { User } from '../../types';
import AvatarWithFrame from '../AvatarWithFrame'; // Đường dẫn có thể cần chỉnh lại

interface Props {
  open: boolean;
  onClose: () => void;
  currentUser: User | null;
  onUpdateSuccess: (updatedUser: User) => void;
}

export default function EditProfileDialog({ open, onClose, currentUser, onUpdateSuccess }: Props) {
  const [fullName, setFullName] = useState('');
  const [bio, setBio] = useState('');
  const [className, setClassName] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [previewAvatar, setPreviewAvatar] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  
  const fileInputRef = useRef<HTMLInputElement>(null);
  const BIO_MAX_LENGTH = 160;

  useEffect(() => {
    if (currentUser && open) {
      setFullName(currentUser.fullName || '');
      setBio(currentUser.bio || '');
      setClassName(currentUser.className || '');
      setPreviewAvatar(null);
      setSelectedFile(null);
    }
  }, [currentUser, open]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setSelectedFile(file);
    setPreviewAvatar(URL.createObjectURL(file));
  };

  const handleUnequipFrame = async () => {
    try {
      const res = await api.put('/api/shop/items/unequip');
      showSuccess(res.data.message || "Đã tháo viền Avatar thành công!");
      if (currentUser) {
          onUpdateSuccess({ ...currentUser, currentAvatarFrame: undefined } as any);
      }
    } catch (error) {
      showError("Không thể tháo viền lúc này.");
    }
  };

  const handleSave = async () => {
    if (!fullName.trim()) return showError("Họ và tên không được để trống!");
    setIsLoading(true);
    try {
      const formData = new FormData();
      formData.append('fullName', fullName.trim());
      formData.append('bio', bio.trim().slice(0, BIO_MAX_LENGTH));
      formData.append('className', className.trim());
      if (selectedFile) formData.append('avatar', selectedFile);

      const res = await api.put('/api/auth/profile', formData, {
        // ⭐ Không set Content-Type thủ công — browser tự gắn boundary
        headers: { 'Content-Type': undefined },
        timeout: 60000,
      });
      
      onUpdateSuccess(res.data);
      showSuccess("Cập nhật thành công!");
      onClose();
    } catch (error) {
      showError("Cập nhật thất bại!");
    } finally {
      setIsLoading(false);
    }
  };

  if (!currentUser) return null;

  let avatarSrc = currentUser.avatarUrl || `https://ui-avatars.com/api/?name=${currentUser.fullName}`;
  if (previewAvatar) avatarSrc = previewAvatar;

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h6" fontWeight="bold">Chỉnh sửa trang cá nhân</Typography>
        <IconButton onClick={onClose}><CloseIcon /></IconButton>
      </DialogTitle>
      <Divider />
      
      <DialogContent sx={{ px: { xs: 2, md: 4 } }}>
        {/* Avatar Section */}
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', mb: 4, mt: 1 }}>
          <Box sx={{ position: 'relative' }}>
            <AvatarWithFrame src={avatarSrc} frameClass={(currentUser as any).currentAvatarFrame} size={120} />
            <IconButton 
              onClick={() => fileInputRef.current?.click()}
              sx={{ position: 'absolute', bottom: 0, right: 0, bgcolor: 'background.paper', boxShadow: 2, '&:hover': { bgcolor: 'action.hover' } }}
            >
              <PhotoCameraIcon fontSize="small" />
            </IconButton>
            <input type="file" hidden accept="image/*" ref={fileInputRef} onChange={handleFileChange} />
          </Box>
          
          {(currentUser as any).currentAvatarFrame && (
            <Button variant="outlined" color="error" size="small" sx={{ mt: 2, borderRadius: 5, textTransform: 'none' }} onClick={handleUnequipFrame}>
              🚫 Tháo viền Avatar
            </Button>
          )}
        </Box>

        {/* Inputs */}
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
          <TextField label="Họ và tên" fullWidth value={fullName} onChange={(e) => setFullName(e.target.value)} />
          <TextField label="Lớp / Khoa" fullWidth value={className} onChange={(e) => setClassName(e.target.value)} />
          <TextField 
            label="Tiểu sử" fullWidth multiline rows={3} 
            value={bio} onChange={(e) => setBio(e.target.value)} 
            helperText={`${bio.length}/${BIO_MAX_LENGTH}`}
            FormHelperTextProps={{ sx: { textAlign: 'right', color: bio.length > BIO_MAX_LENGTH - 20 ? 'error.main' : 'text.secondary' } }}
          />
        </Box>
      </DialogContent>

      <DialogActions sx={{ p: 3, pt: 1 }}>
        <Button onClick={onClose} color="inherit">Hủy</Button>
        <Button variant="contained" onClick={handleSave} disabled={isLoading}>
          {isLoading ? <CircularProgress size={24} color="inherit" /> : "Lưu thay đổi"}
        </Button>
      </DialogActions>
    </Dialog>
  );
}