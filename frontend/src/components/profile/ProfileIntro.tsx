import React from 'react';
import { Paper, Typography, Box, Button } from '@mui/material';
import HomeIcon from '@mui/icons-material/Home';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import InfoIcon from '@mui/icons-material/Info'; // Icon Bio
import type { User } from '../../types';

interface Props {
  user: User | null;
  isSelfProfile: boolean;
  onEditClick: () => void;
}

export default function ProfileIntro({ user, isSelfProfile, onEditClick }: Props) {
  if (!user) return null;

  return (
    <Paper variant="outlined" sx={{ p: 2, border: 'none', boxShadow: '0 1px 2px rgba(0,0,0,0.1)', borderRadius: 2, bgcolor: 'background.paper' }}>
      <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 2 }}>Giới thiệu</Typography>
      
      {/* BIO */}
      {user.bio && (
        <Typography variant="body1" sx={{ textAlign: 'center', mb: 2 }}>
          {user.bio}
        </Typography>
      )}

      {/* CLASS NAME */}
      {user.className && (
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 1.5 }}>
          <InfoIcon color="disabled" sx={{ mr: 1.5 }} />
          <Typography variant="body2">Học tại <b>{user.className}</b></Typography>
        </Box>
      )}

      {/* LOCATION (Giả định hoặc thêm trường sau này) */}
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 1.5 }}>
        <HomeIcon color="disabled" sx={{ mr: 1.5 }} />
        <Typography variant="body2">Sống tại <b>Thành phố Hồ Chí Minh</b></Typography>
      </Box>
      
      {/* JOINED DATE */}
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
        <AccessTimeIcon color="disabled" sx={{ mr: 1.5 }} />
        <Typography variant="body2">
           Tham gia vào {user.createdAt ? new Date(user.createdAt).toLocaleDateString('vi-VN', { month: 'long', year: 'numeric' }) : '...'}
        </Typography>
      </Box>
      
      {isSelfProfile && (
        <Button 
          fullWidth variant="contained" 
          onClick={onEditClick}
          sx={{ bgcolor: '#E4E6E9', color: 'black', fontWeight: 600, textTransform: 'none', '&:hover': { bgcolor: '#D8DADF' } }}
        >
          Chỉnh sửa chi tiết
        </Button>
      )}
    </Paper>
  );
}