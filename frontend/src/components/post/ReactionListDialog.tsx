import React, { useState, useEffect } from 'react';
import {
  Dialog, DialogTitle, DialogContent, IconButton,
  Tabs, Tab, Box, Typography, List, ListItem,
  ListItemAvatar, ListItemText, CircularProgress, Button, Badge
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import { Link as RouterLink } from 'react-router-dom';
import api from '../../api/api';
import AvatarWithFrame from '../AvatarWithFrame';
import ColoredName from '../ColoredName';
import type { ReactionType } from './CardPost';
import icons from 'react-reactions/src/helpers/icons'; // 🔴 IMPORT BỘ ICON

// 🔴 CONFIG SỬ DỤNG ICON KEY
const REACTION_CONFIG: Record<ReactionType, { iconKey: string }> = {
  LIKE: { iconKey: 'like' },
  LOVE: { iconKey: 'love' },
  HAHA: { iconKey: 'haha' },
  WOW: { iconKey: 'wow' },
  SAD: { iconKey: 'sad' },
  ANGRY: { iconKey: 'angry' }
};

const getReactionImg = (type: ReactionType) => icons.find('facebook', REACTION_CONFIG[type]?.iconKey || 'like');

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

  const availableTabs = [
    { value: 'ALL', label: 'Tất cả', count: totalReactions, iconType: null },
    ...Object.entries(reactionCounts)
      .filter(([, count]) => count > 0)
      .sort((a, b) => b[1] - a[1])
      .map(([type, count]) => ({
        value: type,
        label: '',
        count: count,
        iconType: type as ReactionType
      }))
  ];

  const fetchReactions = async (tab: string, pageNum: number) => {
    setLoading(true);
    try {
      const params: any = { page: pageNum, size: 10 };
      if (tab !== 'ALL') {
        params.type = tab;
      }

      const response = await api.get(`/api/posts/${postId}/reactions`, { params });
      const newUsers = response.data.content;
      
      setUsers(prev => pageNum === 0 ? newUsers : [...prev, ...newUsers]);
      setHasMore(!response.data.last);
    } catch (error) {
      console.error("Lỗi khi tải danh sách tương tác:", error);
    } finally {
      setLoading(false);
    }
  };

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
                  {/* 🔴 TAB ICON */}
                  {tab.iconType && (
                    <Box
                      sx={{
                        width: 18, height: 18,
                        backgroundImage: `url(${getReactionImg(tab.iconType)})`,
                        backgroundSize: 'contain', backgroundPosition: 'center', backgroundRepeat: 'no-repeat'
                      }}
                    />
                  )}
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
              to={`/profile/${user.studentCode}`}
              sx={{ '&:hover': { bgcolor: 'action.hover' }, textDecoration: 'none', color: 'inherit' }}
            >
              <ListItemAvatar>
                <Badge
                  overlap="circular"
                  anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
                  badgeContent={
                    // 🔴 BADGE ICON TRÊN AVATAR
                    <Box sx={{
                      width: 18, height: 18,
                      borderRadius: '50%',
                      bgcolor: 'background.paper',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      boxShadow: '0 0 0 2px #fff',
                      backgroundImage: `url(${getReactionImg(user.reactionType)})`,
                      backgroundSize: 'contain', backgroundPosition: 'center', backgroundRepeat: 'no-repeat',
                      zIndex: 2,
                    }} />
                  }
                  sx={{ '& .MuiBadge-badge': { zIndex: 2 } }}>
                  <AvatarWithFrame src={user.avatarUrl} name={user.fullName} size={40} />
                </Badge>
              </ListItemAvatar>
              <ListItemText primary={<ColoredName name={user.fullName} />} />
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