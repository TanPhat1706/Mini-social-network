import React from 'react';
import { Link as RouterLink, useLocation } from 'react-router-dom';
import { Box, List, ListItemButton, ListItemIcon, ListItemText, Divider, Typography } from '@mui/material';
import {
  People as PeopleIcon,
  Groups as GroupsIcon,
  History as HistoryIcon,
  Bookmark as BookmarkIcon,
  SportsEsports as GameIcon,
  Storefront as StorefrontIcon
} from '@mui/icons-material';
import type { User } from '../types/index';

// 🔴 IMPORT MA THUẬT CSS VÀO ĐÂY
import AvatarWithFrame from './AvatarWithFrame';
import ColoredName from '../components/ColoredName'; // (Sửa đường dẫn cho đúng từng file)

interface LeftSidebarProps {
  user: User | null;
}

const menuItems = [
  { text: 'Cửa hàng', icon: <StorefrontIcon sx={{ color: '#F5A623' }} />, path: '/shop' }, { text: 'Nhóm', icon: <GroupsIcon sx={{ color: '#2ABBA7' }} />, path: '/groups' },
  { text: 'Kỷ niệm', icon: <HistoryIcon sx={{ color: '#1877F2' }} />, path: '/history' },
  { text: 'Mini game', icon: <GameIcon sx={{ color: '#F35E7A' }} />, path: '/games' },
  { text: 'Đã lưu', icon: <BookmarkIcon sx={{ color: '#9360F7' }} />, path: '/saved' },
];

export default function LeftSidebar({ user }: LeftSidebarProps) {
  const location = useLocation();

  return (
    <Box sx={{ position: 'sticky', top: '76px', height: 'calc(100vh - 76px)', overflowY: 'auto', pr: 1 }}>
      <List component="nav" sx={{ p: 0 }}>

        {/* THÔNG TIN USER THẬT VÀ AVATAR CÓ VIỀN */}
        <ListItemButton component={RouterLink} to={`/profile`} sx={{ borderRadius: 2, mb: 1, py: 1.5 }}>
          {/* Mở rộng minWidth một chút để viền to như Hoàng Kim có không gian xoay */}
          <ListItemIcon sx={{ minWidth: '55px' }}>
            <AvatarWithFrame
              src={user?.avatarUrl || `https://ui-avatars.com/api/?name=${user?.fullName}`}
              frameClass={(user as any)?.currentAvatarFrame}
              size={40} // Kích thước nhỏ gọn cho Sidebar
            />
          </ListItemIcon>
          <ListItemText
            primary={
              <ColoredName
                name={user?.fullName || 'Người dùng'}
                colorClass={(user as any)?.currentNameColor}
              />
            }
            primaryTypographyProps={{ fontWeight: 600, fontSize: '15px' }}
          />
        </ListItemButton>

        {/* DANH SÁCH MENU */}
        {menuItems.map((item) => {
          const isActive = location.pathname === item.path;

          return (
            <ListItemButton
              key={item.text}
              component={RouterLink}
              to={item.path}
              selected={isActive}
              sx={{
                borderRadius: 2,
                mb: 0.5,
                backgroundColor: isActive ? 'rgba(0, 0, 0, 0.08)' : 'transparent',
                '&.Mui-selected': {
                  backgroundColor: 'rgba(0, 0, 0, 0.08)',
                }
              }}
            >
              <ListItemIcon>{item.icon}</ListItemIcon>
              <ListItemText primary={item.text} primaryTypographyProps={{ fontWeight: 500, fontSize: '15px' }} />
            </ListItemButton>
          );
        })}

        <Divider sx={{ my: 1.5, mx: 2 }} />
        <Typography variant="caption" color="text.secondary" sx={{ pl: 2, fontSize: '12px', display: 'block', lineHeight: 1.5 }}>
          Quyền riêng tư  · Điều khoản  · Quảng cáo  · Cookie  · Meta © 2026
        </Typography>
      </List>
    </Box>
  );
}