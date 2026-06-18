import React from 'react';
import { Dialog, DialogContent, IconButton, Box } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import PostCard, { type PostData } from '../../components/post/CardPost';
import AvatarWithFrame from '../AvatarWithFrame';

interface PostViewModalProps {
  open: boolean;
  onClose: () => void;
  imageUrl?: string;
  frameClass?: string;
  post?: PostData;
}

export default function PostViewModal({ open, onClose, imageUrl, frameClass, post }: PostViewModalProps) {
  return (
    <Dialog 
      open={open} 
      onClose={onClose} 
      maxWidth="sm" // Đổi thành sm để modal không quá thô kệch
      fullWidth
      PaperProps={{
          sx: { bgcolor: 'background.default', borderRadius: 2 }
      }}
    >
      <IconButton
        onClick={onClose}
        sx={{
          position: 'absolute', right: 8, top: 8,
          bgcolor: 'rgba(0,0,0,0.3)', color: 'white',
          zIndex: 10,
          '&:hover': { bgcolor: 'rgba(0,0,0,0.5)' }
        }}
      >
        <CloseIcon />
      </IconButton>
      
      <DialogContent sx={{ 
          p: 0, 
          display: 'flex', 
          justifyContent: 'center', 
          alignItems: 'center', 
          overflow: 'hidden' // 🟢 CHỐNG THANH TRƯỢT
      }}>
        {imageUrl ? (
          <Box sx={{ 
              p: 2, 
              width: '100%', 
              maxHeight: '80vh', // Ép chiều cao tối đa của ảnh
              display: 'flex', 
              justifyContent: 'center' 
          }}>
            <AvatarWithFrame src={imageUrl} frameClass={frameClass} size={400} />
          </Box>
        ) : post ? (
          <Box sx={{ width: '100%' }}>
            <PostCard post={post} onDeleteSuccess={() => onClose()} />
          </Box>
        ) : null}
      </DialogContent>
    </Dialog>
  );
}