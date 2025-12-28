import React, { useState } from 'react';
import { 
  IconButton, Badge, Menu, MenuItem, 
  Typography, Box, Avatar, Divider, Button, CircularProgress 
} from '@mui/material';
import NotificationsIcon from '@mui/icons-material/Notifications';
import { useNavigate } from 'react-router-dom';
import { useWebSocket } from '../../context/WebSocketContext';
import axiosClient from '../../api/axiosClient';

export default function NotificationBell() {
  const { notifications, unreadCount, setUnreadCount } = useWebSocket();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  
  // State lưu trạng thái xử lý của từng nút (để hiện loading xoay xoay)
  const [processingIds, setProcessingIds] = useState<Set<number>>(new Set());

  // State lưu trạng thái quan hệ bạn bè của các người gửi (để biết nên hiện nút hay ẩn nút)
  // Dạng: { [userId]: 'ACCEPTED' | 'PENDING' | 'NONE' }
  const [friendStatuses, setFriendStatuses] = useState<Record<number, string>>({});
  
  const navigate = useNavigate();
  const open = Boolean(anchorEl);

  // --- 1. XỬ LÝ KHI MỞ MENU THÔNG BÁO ---
  const handleClick = async (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
    
    // Reset số lượng chưa đọc
    if (unreadCount > 0) {
      setUnreadCount(0);
    }

    // --- LOGIC QUAN TRỌNG: Kiểm tra lại trạng thái bạn bè của các thông báo đang hiển thị ---
    // Mục đích: Để khi F5 lại trang, nếu đã là bạn bè rồi thì không hiện nút "Chấp nhận" nữa
    
    // Lọc ra danh sách ID những người gửi lời mời kết bạn trong list thông báo
    const friendRequestSenders = notifications
        .filter(n => n.type === 'FRIEND_REQUEST')
        .map(n => n.senderId);
    
    // Loại bỏ ID trùng lặp
    const uniqueSenders = [...new Set(friendRequestSenders)];

    if (uniqueSenders.length > 0) {
        const statuses: Record<number, string> = {};
        
        // Gọi API kiểm tra trạng thái song song cho nhanh
        await Promise.all(uniqueSenders.map(async (id) => {
            try {
                // Gọi API check status (Backend phải có API này)
                const res = await axiosClient.get(`/friends/status/${id}`);
                statuses[id] = res.data.status; 
            } catch (e) { 
                console.error("Lỗi check status:", e); 
            }
        }));

        // Cập nhật vào state để UI tự render lại
        setFriendStatuses(prev => ({ ...prev, ...statuses }));
    }
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleNotificationClick = (notif: any) => {
    // Nếu là lời mời kết bạn -> Không chuyển trang, để user bấm nút
    if (notif.type === 'FRIEND_REQUEST') return; 
    
    handleClose();
    // Chuyển hướng đến link đích (nếu có)
    if (notif.targetUrl && notif.targetUrl !== '#') {
      navigate(notif.targetUrl);
    }
  };

  // --- 2. XỬ LÝ KHI BẤM NÚT CHẤP NHẬN / TỪ CHỐI ---
  const handleFriendAction = async (e: React.MouseEvent, notif: any, action: 'accept' | 'deny') => {
    e.stopPropagation(); // Chặn click lan ra ngoài (tránh kích hoạt handleNotificationClick)
    
    // Đánh dấu đang xử lý để hiện loading
    setProcessingIds(prev => new Set(prev).add(notif.id));

    try {
        if (action === 'accept') {
            await axiosClient.post(`/friends/accept/${notif.senderId}`);
            // Cập nhật ngay trạng thái local thành ACCEPTED -> Nút sẽ biến mất ngay lập tức
            setFriendStatuses(prev => ({ ...prev, [notif.senderId]: 'ACCEPTED' }));
        } else {
            await axiosClient.delete(`/friends/remove/${notif.senderId}`);
            // Cập nhật ngay trạng thái local thành DELETED/NONE
            setFriendStatuses(prev => ({ ...prev, [notif.senderId]: 'DELETED' }));
        }
    } catch (error) {
        console.error("Lỗi xử lý kết bạn:", error);
        alert("Có lỗi xảy ra, vui lòng thử lại.");
    } finally {
        // Tắt loading
        setProcessingIds(prev => {
            const next = new Set(prev);
            next.delete(notif.id);
            return next;
        });
    }
  };

  return (
    <>
      <IconButton color="inherit" onClick={handleClick} sx={{ mr: 1 }}>
        <Badge badgeContent={unreadCount} color="error">
          <NotificationsIcon />
        </Badge>
      </IconButton>

      <Menu
        anchorEl={anchorEl}
        open={open}
        onClose={handleClose}
        PaperProps={{
          elevation: 0,
          sx: {
            overflow: 'visible', // Để hiện mũi tên nhọn
            filter: 'drop-shadow(0px 2px 8px rgba(0,0,0,0.32))',
            mt: 1.5,
            width: 360,
            '&:before': {
              content: '""',
              display: 'block',
              position: 'absolute',
              top: 0, right: 14, width: 10, height: 10,
              bgcolor: 'background.paper',
              transform: 'translateY(-50%) rotate(45deg)',
              zIndex: 0,
            },
          },
        }}
        transformOrigin={{ horizontal: 'right', vertical: 'top' }}
        anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
      >
        {/* HEADER CỐ ĐỊNH */}
        <Box sx={{ p: 2, pb: 1 }}>
            <Typography variant="h6">Thông báo</Typography>
        </Box>
        <Divider />

        {/* DANH SÁCH CUỘN (Scrollable Area) */}
        <Box sx={{ maxHeight: '400px', overflowY: 'auto' }}>
            {notifications.length === 0 ? (
            <MenuItem disabled>
                <Typography variant="body2" color="text.secondary">Không có thông báo mới</Typography>
            </MenuItem>
            ) : (
            notifications.map((notif, index) => {
                // --- LOGIC QUYẾT ĐỊNH HIỂN THỊ NÚT HAY CHỮ ---
                const status = friendStatuses[notif.senderId];
                
                // Mặc định là hiện nút nếu chưa có status (PENDING)
                // Ẩn nút nếu đã ACCEPTED, DELETED hoặc đã hủy (NONE)
                const showButtons = notif.type === 'FRIEND_REQUEST' && 
                                    (status === undefined || status === 'PENDING');

                // Nội dung phản hồi khi đã xử lý
                let feedbackText = '';
                if (status === 'ACCEPTED') feedbackText = 'Đã trở thành bạn bè';
                else if (status === 'DELETED' || status === 'NONE') feedbackText = 'Đã từ chối lời mời';

                return (
                    <MenuItem 
                        key={notif.id || index}
                        onClick={() => handleNotificationClick(notif)}
                        sx={{ 
                            whiteSpace: 'normal',
                            bgcolor: !notif.isRead ? 'rgba(79, 70, 229, 0.08)' : 'inherit',
                            display: 'flex', alignItems: 'flex-start', gap: 1.5, py: 1.5,
                            borderBottom: '1px solid #f0f0f0',
                            cursor: notif.type === 'FRIEND_REQUEST' ? 'default' : 'pointer'
                        }} 
                    >
                    <Avatar src={notif.senderAvatar} alt={notif.senderName} sx={{ width: 48, height: 48 }} />
                    
                    <Box sx={{ flex: 1 }}>
                        <Typography variant="subtitle2" component="div">
                            <strong>{notif.senderName}</strong> {notif.message}
                        </Typography>
                        <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1 }}>
                            {new Date(notif.createdAt).toLocaleString()}
                        </Typography>

                        {/* --- KHU VỰC HIỂN THỊ NÚT / TEXT PHẢN HỒI --- */}
                        {notif.type === 'FRIEND_REQUEST' && (
                            <Box sx={{ mt: 1 }}>
                                {showButtons ? (
                                    <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                                        {/* Nếu đang xử lý thì hiện vòng xoay */}
                                        {processingIds.has(notif.id) ? (
                                            <CircularProgress size={20} />
                                        ) : (
                                            <>
                                                <Button 
                                                    variant="contained" size="small" 
                                                    onClick={(e) => handleFriendAction(e, notif, 'accept')}
                                                    sx={{ bgcolor: '#1877F2', fontSize: '12px', textTransform: 'none' }}
                                                >
                                                    Chấp nhận
                                                </Button>
                                                <Button 
                                                    variant="outlined" size="small" color="inherit"
                                                    onClick={(e) => handleFriendAction(e, notif, 'deny')}
                                                    sx={{ fontSize: '12px', textTransform: 'none' }}
                                                >
                                                    Từ chối
                                                </Button>
                                            </>
                                        )}
                                    </Box>
                                ) : (
                                    // Nếu đã xử lý thì hiện text (VD: Đã trở thành bạn bè)
                                    <Typography variant="caption" sx={{ fontWeight: 'bold', color: status === 'ACCEPTED' ? 'green' : 'grey' }}>
                                        {feedbackText}
                                    </Typography>
                                )}
                            </Box>
                        )}
                    </Box>

                    {/* Chấm xanh chưa đọc */}
                    {!notif.isRead && (
                        <Box sx={{ width: 10, height: 10, bgcolor: '#1877F2', borderRadius: '50%', mt: 1 }} />
                    )}
                    </MenuItem>
                );
            })
            )}
        </Box>
      </Menu>
    </>
  );
}