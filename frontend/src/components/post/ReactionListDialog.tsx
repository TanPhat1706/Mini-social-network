import React, { useState, useEffect } from 'react';
import {
  Dialog, DialogTitle, DialogContent, IconButton,
  Tabs, Tab, Box, Typography, List, ListItem,
  ListItemAvatar, ListItemText, CircularProgress, Button, Avatar, Badge
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import { Link as RouterLink } from 'react-router-dom';
import api from '../../api/api';
import AvatarWithFrame from '../AvatarWithFrame';
import ColoredName from '../ColoredName';
import type { ReactionType } from './CardPost'

const REACTION_CONFIG: Record<ReactionType, { emoji: string; color: string }> = {
  LIKE: { emoji: '👍', color: 'primary.main' },
  LOVE: { emoji: '❤️', color: 'error.main' },
  HAHA: { emoji: '😂', color: 'warning.dark' },
  WOW: { emoji: '😮', color: 'warning.main' },
  SAD: { emoji: '😢', color: 'info.main' },
  ANGRY: { emoji: '😡', color: 'error.dark' }
};

interface ReactionUser {
  userId: number;
  studentCode: string;
  fullName: string;
  avatarUrl: string;
  reactionType: ReactionType;
}

interface ReactionListDialogProps {
  open: boolean;
  onClose: () => void;
  postId: number;
  reactionCounts: Record<string, number>;
  totalReactions: number;
}

export default function ReactionListDialog({ open, onClose, postId, reactionCounts, totalReactions }: ReactionListDialogProps) {
  const [currentTab, setCurrentTab] = useState<string>('ALL');
  const [users, setUsers] = useState<ReactionUser[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);

  // Khởi tạo danh sách các Tab dựa trên những cảm xúc đang có thật trong bài viết
  const availableTabs = [
    { value: 'ALL', label: 'Tất cả', count: totalReactions, emoji: '' },
    ...Object.entries(reactionCounts)
      .filter(([, count]) => count > 0)
      .sort((a, b) => b[1] - a[1]) // Sắp xếp giảm dần theo số lượng
      .map(([type, count]) => ({
        value: type,
        label: '',
        count: count,
        emoji: REACTION_CONFIG[type as ReactionType]?.emoji || ''
      }))
  ];

  const fetchReactions = async (tab: string, pageNum: number) => {
    setLoading(true);
    try {
      // Nếu tab là ALL, không truyền params type
      const params: any = { page: pageNum, size: 10 };
      if (tab !== 'ALL') {
        params.type = tab;
      }

      const response = await api.get(`/api/posts/${postId}/reactions`, { params });
      const newUsers = response.data.content; // Phụ thuộc vào cấu trúc Page<T> của Spring Boot
      
      setUsers(prev => pageNum === 0 ? newUsers : [...prev, ...newUsers]);
      setHasMore(!response.data.last); // Spring Boot Page trả về field 'last' (boolean)
    } catch (error) {
      console.error("Lỗi khi tải danh sách tương tác:", error);
    } finally {
      setLoading(false);
    }
  };

  // Mỗi khi đổi Tab hoặc mở Dialog, reset lại dữ liệu và gọi API trang 0
  useEffect(() => {
    if (open) {
      setPage(0);
      setUsers([]);
      fetchReactions(currentTab, 0);
    }
  }, [open, currentTab, postId]);

  const handleTabChange = (event: React.SyntheticEvent, newValue: string) => {
    setCurrentTab(newValue);
  };

  const handleLoadMore = () => {
    if (!loading && hasMore) {
      const nextPage = page + 1;
      setPage(nextPage);
      fetchReactions(currentTab, nextPage);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="xs" scroll="paper">
      <DialogTitle sx={{ m: 0, p: 2, display: 'flex', alignItems: 'center', borderBottom: 1, borderColor: 'divider' }}>
        <Tabs 
          value={currentTab} 
          onChange={handleTabChange} 
          variant="scrollable" 
          scrollButtons="auto"
          sx={{ minHeight: 36, '& .MuiTab-root': { minHeight: 36, py: 0, px: 2, textTransform: 'none' } }}
        >
          {availableTabs.map((tab) => (
            <Tab 
              key={tab.value} 
              value={tab.value} 
              label={
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                  {tab.emoji && <Typography sx={{ fontSize: 16 }}>{tab.emoji}</Typography>}
                  <Typography variant="body2" sx={{ fontWeight: currentTab === tab.value ? 'bold' : 'normal' }}>
                    {tab.label || tab.count}
                  </Typography>
                </Box>
              } 
            />
          ))}
        </Tabs>
        <IconButton onClick={onClose} sx={{ position: 'absolute', right: 8, top: 8, color: 'text.secondary' }}>
          <CloseIcon />
        </IconButton>
      </DialogTitle>
      
      <DialogContent dividers sx={{ p: 0, minHeight: 300 }}>
        <List sx={{ pt: 0 }}>
          {users.map((user, index) => (
            <ListItem 
              key={`${user.userId}-${index}`} 
              component={RouterLink} 
              to={`/profile/${user.studentCode}`} // Chuyển hướng tới trang cá nhân
              sx={{ '&:hover': { bgcolor: 'action.hover' }, textDecoration: 'none', color: 'inherit' }}
            >
              <ListItemAvatar>
                <Badge
                  overlap="circular"
                  anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
                  badgeContent={
                    <Box sx={{ 
                      width: 16, height: 16, borderRadius: '50%', bgcolor: 'background.paper', 
                      display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: 1,
                      zIndex: 2
                    }}>
                      <Typography sx={{ fontSize: 10 }}>
                        {REACTION_CONFIG[user.reactionType]?.emoji}
                      </Typography>
                    </Box>
                  }
                  sx={{ '& .MuiBadge-badge': { zIndex: 2 } }}>
                  <AvatarWithFrame src={user.avatarUrl} name={user.fullName} size={40} />
                </Badge>
              </ListItemAvatar>
              <ListItemText 
                primary={
                  <ColoredName name={user.fullName} /> // Nếu em có xử lý màu tên
                } 
              />
            </ListItem>
          ))}
        </List>
        
        {loading && (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
            <CircularProgress size={24} />
          </Box>
        )}
        
        {!loading && hasMore && users.length > 0 && (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
            <Button variant="text" size="small" onClick={handleLoadMore}>
              Tải thêm
            </Button>
          </Box>
        )}

        {!loading && users.length === 0 && (
          <Box sx={{ p: 4, textAlign: 'center' }}>
            <Typography variant="body2" color="text.secondary">Chưa có tương tác nào.</Typography>
          </Box>
        )}
      </DialogContent>
    </Dialog>
  );
}