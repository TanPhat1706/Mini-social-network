import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Button, Box, CircularProgress, Menu, MenuItem, ListItemIcon, ListItemText } from '@mui/material';
import CheckIcon from '@mui/icons-material/Check';
import PersonRemoveIcon from '@mui/icons-material/PersonRemove';
import { useTheme } from '@mui/material/styles';
import { showError, showSuccess } from '../../utils/swal';
import { getFriendshipStatus, sendFriendRequest, removeFriendship } from '../../api/friendApi';

interface Props {
  targetUserId: number;
  currentUserId: number;
  className?: string;
}

const FriendButton: React.FC<Props> = ({ targetUserId, currentUserId, className }) => {
  const [status, setStatus] = useState<string>('NONE');
  const [actionUserId, setActionUserId] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [isFetchingStatus, setIsFetchingStatus] = useState<boolean>(true);

  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const openMenu = Boolean(anchorEl);

  const theme = useTheme();

  useEffect(() => {
    let isMounted = true;

    const fetchStatus = async () => {
      setIsFetchingStatus(true);

      try {
        const response = await getFriendshipStatus(targetUserId);
        if (!isMounted) return;

        setStatus(response.data.status ?? 'NONE');
        setActionUserId(response.data.actionUserId ?? null);
      } catch (error) {
        console.error(error);
        if (isMounted) {
          showError('Không thể tải trạng thái kết bạn. Vui lòng thử lại sau.');
          setStatus('NONE');
          setActionUserId(null);
        }
      } finally {
        if (isMounted) setIsFetchingStatus(false);
      }
    };

    fetchStatus();

    return () => {
      isMounted = false;
    };
  }, [targetUserId]);

  const handleAddFriend = async () => {
    setIsLoading(true);
    try {
      await sendFriendRequest(targetUserId);
      setStatus('PENDING');
      setActionUserId(currentUserId);
    } catch (error) {
      console.error(error);
      const errorMessage = axios.isAxiosError(error)
        ? (error.response?.data?.message || error.response?.data || error.message)
        : 'Gửi lời mời kết bạn thất bại. Vui lòng thử lại.';
      showError(String(errorMessage));
    } finally {
      setIsLoading(false);
    }
  };

  const handleRemoveFriend = async () => {
    setAnchorEl(null);
    setIsLoading(true);
    try {
      await removeFriendship(targetUserId);
      setStatus('NONE');
      setActionUserId(null);
      showSuccess('Đã hủy kết bạn.');
    } catch (error) {
      console.error(error);
      showError('Không thể hủy kết bạn lúc này.');
    } finally {
      setIsLoading(false);
    }
  };

  if (isFetchingStatus) {
    return (
      <Button
        fullWidth
        variant="contained"
        disabled
        className={className}
        sx={{ textTransform: 'none', fontWeight: 600, height: 36 }}
        startIcon={<CircularProgress size={16} color="inherit" />}
      >
        Đang tải...
      </Button>
    );
  }

  if (status === 'ACCEPTED') {
    return (
      <>
        <Button
          fullWidth
          variant="contained"
          className={className}
          onClick={(e) => setAnchorEl(e.currentTarget)}
          disabled={isLoading}
          sx={{
            textTransform: 'none',
            fontWeight: 600,
            height: 36,
            bgcolor: 'success.main',
            color: 'common.white',
            '&:hover': { bgcolor: 'success.dark' },
          }}
          startIcon={isLoading ? <CircularProgress size={16} color="inherit" /> : <CheckIcon fontSize="small" />}
        >
          Bạn bè
        </Button>
        <Menu
          anchorEl={anchorEl}
          open={openMenu}
          onClose={() => setAnchorEl(null)}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
        >
          <MenuItem onClick={handleRemoveFriend} sx={{ color: 'error.main' }}>
            <ListItemIcon>
              <PersonRemoveIcon fontSize="small" color="error" />
            </ListItemIcon>
            <ListItemText primary="Hủy kết bạn" />
          </MenuItem>
        </Menu>
      </>
    );
  }

  if (status === 'PENDING') {
    return (
      <>
        <Button
          fullWidth
          variant="outlined"
          className={className}
          onClick={(e) => setAnchorEl(e.currentTarget)}
          disabled={isLoading}
          sx={{
            textTransform: 'none',
            fontWeight: 600,
            height: 36,
            borderColor: theme.palette.divider,
            color: theme.palette.text.secondary,
          }}
        >
          {isLoading ? <CircularProgress size={16} color="inherit" /> : 'Đã gửi lời mời'}
        </Button>
        <Menu
          anchorEl={anchorEl}
          open={openMenu}
          onClose={() => setAnchorEl(null)}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
        >
          <MenuItem onClick={handleRemoveFriend}>
            <ListItemIcon><PersonRemoveIcon fontSize="small" /></ListItemIcon>
            <ListItemText primary="Hủy lời mời" />
          </MenuItem>
        </Menu>
      </>
    );
  }

  return (
    <Box sx={{ width: '100%' }}>
      <Button
        fullWidth
        variant="contained"
        color="primary"
        onClick={handleAddFriend}
        disabled={isLoading}
        className={className}
        sx={{ textTransform: 'none', fontWeight: 600, height: 36 }}
        startIcon={isLoading ? <CircularProgress size={16} color="inherit" /> : undefined}
      >
        {isLoading ? 'Đang gửi...' : 'Thêm bạn'}
      </Button>
    </Box>
  );
};

export default FriendButton;
