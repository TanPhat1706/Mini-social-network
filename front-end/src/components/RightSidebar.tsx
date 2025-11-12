import React from 'react';
import { 
  Box, List, ListItem, ListItemButton, ListItemIcon, 
  ListItemText, Avatar, Divider, Typography, Paper, Badge
} from '@mui/material';
import { styled } from '@mui/material/styles';

// --- Tạo chấm "Online" ---
// Đây là cách "tỉ mỉ" để tạo chấm online
const OnlineBadge = styled(Badge)(({ theme }) => ({
  '& .MuiBadge-badge': {
    backgroundColor: '#44b700', // Màu xanh lá cây
    color: '#44b700',
    boxShadow: `0 0 0 2px ${theme.palette.background.paper}`,
    '&::after': {
      position: 'absolute',
      top: 0,
      left: 0,
      width: '100%',
      height: '100%',
      borderRadius: '50%',
      animation: 'ripple 1.2s infinite ease-in-out',
      border: '1px solid currentColor',
      content: '""',
    },
  },
  '@keyframes ripple': {
    '0%': {
      transform: 'scale(.8)',
      opacity: 1,
    },
    '100%': {
      transform: 'scale(2.4)',
      opacity: 0,
    },
  },
}));

// --- Mock Data Danh sách bạn bè ---
const friends = [
  { name: 'Nguyễn Văn A', online: true, avatar: '/static/1.jpg' },
  { name: 'Trần Thị B', online: true, avatar: '/static/2.jpg' },
  { name: 'Lê Văn C', online: false, avatar: '/static/3.jpg' },
  { name: 'Phạm Thị D', online: true, avatar: '/static/4.jpg' },
  { name: 'Võ Văn E', online: false, avatar: '/static/5.jpg' },
];
// ---------------------------------

export default function RightSidebar() {
  return (
    // Box này cũng sẽ 'dính' lại khi cuộn
    <Box sx={{ 
      position: 'sticky', 
      top: (theme) => `calc(${theme.mixins.toolbar.minHeight}px + ${theme.spacing(3)})`,
    }}>
      <Paper elevation={0} sx={{ border: '1px solid #E0E0E0', p: 2 }}>
        <Typography variant="h6" gutterBottom>
          Người liên hệ
        </Typography>
        <List component="nav" disablePadding>
          {friends.map((friend) => (
            <ListItemButton key={friend.name} sx={{ borderRadius: 1.5, p: 1 }}>
              <ListItemIcon sx={{ minWidth: 40, mr: 1 }}>
                {friend.online ? (
                  <OnlineBadge
                    overlap="circular"
                    anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
                    variant="dot"
                  >
                    <Avatar alt={friend.name} src={friend.avatar} />
                  </OnlineBadge>
                ) : (
                  <Avatar alt={friend.name} src={friend.avatar} />
                )}
              </ListItemIcon>
              <ListItemText 
                primary={friend.name}
                primaryTypographyProps={{ fontWeight: 500 }} 
              />
            </ListItemButton>
          ))}
        </List>
      </Paper>
    </Box>
  );
}