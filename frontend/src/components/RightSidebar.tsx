import React, { useState, useEffect } from 'react';
import { Box, List, ListItemButton, ListItemIcon, ListItemText, Typography, Tooltip } from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import MoreHorizIcon from '@mui/icons-material/MoreHoriz';
import type { User } from '../types/types';

import api from '../api/api'; 
import { differenceInMinutes, differenceInHours, differenceInDays } from 'date-fns';

import AvatarWithFrame from './AvatarWithFrame';
import ColoredName from './ColoredName'; 

interface RightSidebarProps {
  friends: User[];
  onFriendClick: (friend: User) => void;
}

interface UserPresence {
  online: boolean;
  lastSeen?: string;
}

// 🟢 HÀM FORMAT THỜI GIAN NGẮN GỌN (Đã bỏ 'vừa xong', đếm thẳng từ 1p)
const formatShortTime = (lastSeen?: string) => {
  if (!lastSeen) return '';
  const lastDate = new Date(lastSeen);
  const mins = differenceInMinutes(new Date(), lastDate);

  // Kể cả dưới 1 phút hoặc bằng 0 thì vẫn hiển thị là 1p cho đẹp giao diện
  if (mins < 60) {
    return `${mins <= 0 ? 1 : mins}p`;
  }

  const hours = differenceInHours(new Date(), lastDate);
  if (hours < 24) return `${hours}g`;

  const days = differenceInDays(new Date(), lastDate);
  if (days < 7) return `${days}n`;

  return ''; 
};

export default function RightSidebar({ friends, onFriendClick }: RightSidebarProps) {
  const [presences, setPresences] = useState<Record<string, UserPresence>>({});

  useEffect(() => {
    if (!friends || friends.length === 0) return;

    const fetchAllPresences = async () => {
      try {
        const promises = friends.map(friend => api.get(`/api/users/${friend.studentCode}/presence`));
        const results = await Promise.allSettled(promises);

        const newPresences: Record<string, UserPresence> = {};
        
        results.forEach((result, index) => {
          if (result.status === 'fulfilled') {
            newPresences[friends[index].studentCode] = result.value.data;
          }
        });

        setPresences(newPresences);
      } catch (error) {
        console.error("Lỗi tải trạng thái bạn bè:", error);
      }
    };

    fetchAllPresences(); 
    const interval = setInterval(fetchAllPresences, 60000); 

    return () => clearInterval(interval);
  }, [friends]);

  return (
    <Box sx={{ position: 'sticky', top: '76px', height: 'calc(100vh - 76px)', overflowY: 'auto' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', px: 1, mb: 1, color: 'text.secondary' }}>
        <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>Người liên hệ</Typography>
        <Box sx={{ display: 'flex', gap: 1 }}>
        </Box>
      </Box>

      <List disablePadding>
        {friends.length > 0 ? (
          friends.map((friend) => {
            const presence = presences[friend.studentCode];
            const isOnline = presence?.online;
            const offlineText = !isOnline ? formatShortTime(presence?.lastSeen) : '';
            
            // 🟢 TÍNH TOÁN MÀU SẮC NỔI BẬT: Nếu chứa chữ 'p' (phút) thì tô màu xanh lá
            const isRecentOffline = offlineText.endsWith('p');

            return (
              <ListItemButton
                key={friend.id}
                onClick={() => onFriendClick(friend)}
                sx={{ borderRadius: 2, p: 1, '&:hover': { backgroundColor: 'action.hover' } }}
              >
                <ListItemIcon sx={{ minWidth: 44 }}>
                  <Box sx={{ position: 'relative', display: 'inline-block' }}>
                    <AvatarWithFrame
                      src={friend.avatarUrl || `https://ui-avatars.com/api/?name=${friend.fullName}`}
                      frameClass={(friend as any).currentAvatarFrame}
                      size={36}
                    />
                    
                    {/* RENDER TRẠNG THÁI NGAY GÓC AVATAR */}
                    {isOnline ? (
                      <Box sx={{
                        position: 'absolute', bottom: -2, right: -2, width: 12, height: 12,
                        backgroundColor: '#31a24c', borderRadius: '50%',
                        border: '2px solid', borderColor: 'background.default', zIndex: 10
                      }} />
                    ) : offlineText ? (
                      <Box sx={{
                        position: 'absolute', bottom: -4, right: -6, 
                        // 🟢 ĐỔI MÀU NỀN VÀ MÀU CHỮ DỰA TRÊN THỜI GIAN OFFLINE
                        backgroundColor: isRecentOffline ? '#e7f3ff' : 'action.selected', 
                        color: isRecentOffline ? '#1877f2' : 'text.secondary', 
                        borderRadius: '10px', 
                        border: '2px solid', borderColor: 'background.default',
                        px: 0.6, 
                        minWidth: '20px', 
                        height: '18px',
                        fontSize: '10px', 
                        fontWeight: 800, // Làm chữ in đậm nổi bật hẳn lên
                        zIndex: 10,
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        boxShadow: '0 2px 4px rgba(0,0,0,0.1)' 
                      }}>
                        {offlineText}
                      </Box>
                    ) : null}
                  </Box>
                </ListItemIcon>

                <ListItemText
                  primary={<ColoredName name={friend.fullName} colorClass={(friend as any).currentNameColor} />}
                  primaryTypographyProps={{ fontWeight: 500, fontSize: '14px', color: 'text.primary' }}
                />
              </ListItemButton>
            );
          })
        ) : (
          <Typography variant="body2" sx={{ px: 2, py: 1, color: 'text.secondary', fontStyle: 'italic' }}>
            Chưa có người liên hệ.
          </Typography>
        )}
      </List>
    </Box>
  );
}