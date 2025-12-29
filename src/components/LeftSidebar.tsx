import React from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { 
  Box, List, ListItemButton, ListItemIcon, 
  ListItemText, Avatar, Divider, Typography, Paper
} from '@mui/material';

// Import Icons
import PeopleIcon from '@mui/icons-material/People';
import GroupsIcon from '@mui/icons-material/Groups';
import HistoryIcon from '@mui/icons-material/History';
import EventIcon from '@mui/icons-material/Event';
import type { User } from '../../types';

interface Props {
  user: User | null; // Nhận dữ liệu từ HomePage
}

export default function LeftSidebar({ user }: Props) {
  const menuItems = [
    { text: 'Bạn bè', icon: <PeopleIcon />, path: '/friends' },
    { text: 'Nhóm', icon: <GroupsIcon />, path: '/groups' },
    { text: 'Kỷ niệm', icon: <HistoryIcon />, path: '/history' },
    { text: 'Sự kiện', icon: <EventIcon />, path: '/events' },
  ];

  return (
    <Box sx={{ position: 'sticky', top: 80 }}>
      <Paper elevation={0} sx={{ border: 'none', bgcolor: 'transparent', p: 1 }}>
        <List component="nav" disablePadding>
          {/* LINK ĐẾN TRANG CÁ NHÂN VỚI DỮ LIỆU THẬT */}
          <ListItemButton 
            component={RouterLink}
            to={`/profile`}
            sx={{ borderRadius: 1.5, mb: 1 }}
          >            
            <ListItemIcon>
              <Avatar 
                src={user?.avatarUrl || `https://ui-avatars.com/api/?name=${user?.fullName}`} 
                sx={{ width: 36, height: 36 }}
              />
            </ListItemIcon>
            <ListItemText 
              primary={user?.fullName || "Đang tải..."} 
              primaryTypographyProps={{ fontWeight: '600', fontSize: '15px' }} 
            />
          </ListItemButton>

          {/* CÁC MENU TÁC VỤ */}
          {menuItems.map((item) => (
            <ListItemButton key={item.text} sx={{ borderRadius: 1.5 }}>
              <ListItemIcon>
                {React.cloneElement(item.icon, { sx: { color: '#1877F2', fontSize: '28px' } })}
              </ListItemIcon>
              <ListItemText primary={item.text} primaryTypographyProps={{ fontSize: '15px', fontWeight: '500' }} />
            </ListItemButton>
          ))}
          
          <Divider sx={{ my: 1, mx: 2 }} />
          <Typography variant="caption" color="text.secondary" sx={{ pl: 2 }}>
            Quyền riêng tư · Điều khoản · Quảng cáo
          </Typography>
        </List>
      </Paper>
    </Box>
  );
}