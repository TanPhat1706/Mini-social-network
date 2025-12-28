import React from 'react';
import { Paper, Typography, Box, Divider, Button } from '@mui/material';
import HomeIcon from '@mui/icons-material/Home'; // Icon nơi ở
import AccessTimeIcon from '@mui/icons-material/AccessTime'; // Icon ngày tham gia

// Mock data cho phần giới thiệu
const mockIntro = {
  location: "Sống tại Thành phố Hồ Chí Minh",
  joined: "Tham gia vào tháng 11 2024",
};

export default function ProfileIntro() {
  return (
    <Paper 
      variant="outlined" 
      sx={{ p: 2, border: '1px solid #E0E0E0', bgcolor: 'background.paper' }}
    >
      <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 2 }}>
        Giới thiệu
      </Typography>
      
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
        <HomeIcon color="disabled" sx={{ mr: 1.5 }} />
        <Typography variant="body2">{mockIntro.location}</Typography>
      </Box>
      
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
        <AccessTimeIcon color="disabled" sx={{ mr: 1.5 }} />
        <Typography variant="body2">{mockIntro.joined}</Typography>
      </Box>
      
      <Button fullWidth variant="contained" sx={{ bgcolor: 'action.hover', color: 'text.primary', '&:hover': { bgcolor: 'action.hover' } }}>
        Chỉnh sửa chi tiết
      </Button>
    </Paper>
  );
}