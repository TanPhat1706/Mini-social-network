import React from 'react';
import { Paper, Typography, Box, Button } from '@mui/material';
import HomeIcon from '@mui/icons-material/Home';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import InfoIcon from '@mui/icons-material/Info'; 
import type { User } from '../../types';

interface Props {
  user: User | null;
  isSelfProfile: boolean;
  onEditClick: () => void;
}

export default function ProfileIntro({ user, isSelfProfile, onEditClick }: Props) {
  if (!user) return null;

  return (
    <Paper elevation={1} sx={{ p: 3, borderRadius: 2 }}>
      <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 2 }}>Giới thiệu</Typography>
      
      {user.bio && (
        <Typography variant="body1" sx={{ textAlign: 'center', mb: 2 }}>{user.bio}</Typography>
      )}

      {user.className && (
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 1.5 }}>
          <InfoIcon color="disabled" sx={{ mr: 1.5 }} />
          <Typography variant="body2">Học tại <b>{user.className}</b></Typography>
        </Box>
      )}

      <Box sx={{ display: 'flex', alignItems: 'center', mb: 1.5 }}>
        <HomeIcon color="disabled" sx={{ mr: 1.5 }} />
        <Typography variant="body2">Sống tại <b>Thành phố Hồ Chí Minh</b></Typography>
      </Box>
      
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
        <AccessTimeIcon color="disabled" sx={{ mr: 1.5 }} />
        <Typography variant="body2">Tham gia vào {user.createdAt ? new Date(user.createdAt).toLocaleDateString('vi-VN', { month: 'long', year: 'numeric' }) : '...'}</Typography>
      </Box>
      
      {isSelfProfile && (
        <Button 
          fullWidth variant="contained" 
          onClick={onEditClick}
          sx={{ bgcolor: 'action.selected', color: 'text.primary', fontWeight: 600, textTransform: 'none', '&:hover': { bgcolor: 'action.hover' } }}
        >
          Chỉnh sửa chi tiết
        </Button>
      )}
    </Paper>
  );
}