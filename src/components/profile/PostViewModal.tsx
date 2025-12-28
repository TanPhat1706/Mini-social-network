import React from 'react';
import { Dialog, DialogContent, IconButton, Box } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import PostCard from '../../components/post/CardPost'; // Tái sử dụng PostCard

// Định nghĩa kiểu cho props
interface PostViewModalProps {
  open: boolean;
  onClose: () => void;
  // TODO: Sau này sẽ truyền post thật vào
  // post: any; 
}

export default function PostViewModal({ open, onClose }: PostViewModalProps) {
  return (
    <Dialog 
      open={open} 
      onClose={onClose} 
      maxWidth="md" 
      fullWidth
    >
      {/* Nút đóng X (giống ảnh 2 của lần trước) */}
      <IconButton
        aria-label="close"
        onClick={onClose}
        sx={{
          position: 'absolute',
          right: 8,
          top: 8,
          color: (theme) => theme.palette.grey[500],
          backgroundColor: 'rgba(255,255,255,0.7)',
          zIndex: 1, // Nằm trên
          '&:hover': {
            backgroundColor: 'rgba(255,255,255,0.9)',
          }
        }}
      >
        <CloseIcon />
      </IconButton>
      
      <DialogContent sx={{ p: 0, m: 0 }}>
        {/* Chúng ta sẽ hiển thị PostCard ngay trong Modal */}
        {/* Sau này, chúng ta sẽ truyền "post" thật vào đây */}
        <PostCard />
      </DialogContent>
    </Dialog>
  );
}