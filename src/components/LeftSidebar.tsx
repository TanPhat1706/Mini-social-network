import React from 'react';
import { Link as RouterLink } from 'react-router-dom';

import { 
  Box, List, ListItem, ListItemButton, ListItemIcon, 
  ListItemText, Avatar, Divider, Typography, Paper
} from '@mui/material';

// Import Icons
import PeopleIcon from '@mui/icons-material/People';
import GroupsIcon from '@mui/icons-material/Groups';
import HistoryIcon from '@mui/icons-material/History';
import EventIcon from '@mui/icons-material/Event';

// Mock data cho người dùng đang đăng nhập
const currentUser = {
  id: "lehonphat",
  name: "Lê Hồng Phát",
  avatarUrl: "https://placehold.co/40x40/EFEFEF/333?text=LHP",
};

// Danh sách các tác vụ
const menuItems = [
    { text: 'Bạn bè', icon: <PeopleIcon />, path: '/friends' },
    { text: 'Nhóm', icon: <GroupsIcon />, path: '/groups' },
    { text: 'Kỷ niệm', icon: <HistoryIcon />, path: '/history' },
    { text: 'Sự kiện', icon: <EventIcon />, path: '/events' },
  ];

export default function LeftSidebar() {
  return (
    // Box này sẽ 'dính' lại khi cuộn
    <Box sx={{ 
      position: 'sticky', 
      // 64px (Header) + 24px (Container mt) = 88px
      top: (theme) => `calc(${theme.mixins.toolbar.minHeight}px + ${theme.spacing(3)})`,
    }}>
      <Paper elevation={0} sx={{ border: '1px solid #E0E0E0', p: 1 }}>
        <List component="nav" disablePadding>
          {/* Link đến trang cá nhân */}
          <ListItemButton 
          component={RouterLink}
          to={`/profile/${currentUser.id}`}
          sx={{ borderRadius: 1.5, mb: 1 }}
        >            
        <ListItemIcon>
              <Avatar 
                src={currentUser.avatarUrl} 
                alt={currentUser.name} 
                sx={{ width: 32, height: 32 }}
              />
            </ListItemIcon>
            <ListItemText 
              primary={currentUser.name} 
              primaryTypographyProps={{ fontWeight: 'bold' }} 
            />
          </ListItemButton>

          {/* Các menu tác vụ */}
          {menuItems.map((item) => (
            <ListItemButton key={item.text} sx={{ borderRadius: 1.5 }}>
              <ListItemIcon>
                {/* Dùng màu primary cho icon */}
                {React.cloneElement(item.icon, { color: 'primary' })}
              </ListItemIcon>
              <ListItemText primary={item.text} />
            </ListItemButton>
          ))}
          
          <Divider sx={{ my: 1 }} />
          
          <Typography variant="caption" color="text.secondary" sx={{ pl: 2 }}>
            (Các lối tắt khác...)
          </Typography>
        </List>
      </Paper>
    </Box>
  );
}