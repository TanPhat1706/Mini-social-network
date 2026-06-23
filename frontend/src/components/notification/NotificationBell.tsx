import React, { useState } from 'react';
import { 
  IconButton, Badge, Menu, MenuItem, 
  Typography, Box, Divider, Button, CircularProgress, Tooltip
} from '@mui/material';
import NotificationsIcon from '@mui/icons-material/Notifications';
import ChatBubbleIcon from '@mui/icons-material/ChatBubble';
import ReplyIcon from '@mui/icons-material/Reply';
import FavoriteIcon from '@mui/icons-material/Favorite';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import ShareIcon from '@mui/icons-material/Share';
import InsertEmoticonIcon from '@mui/icons-material/InsertEmoticon';
import icons from 'react-reactions/src/helpers/icons';
import { useNavigate } from 'react-router-dom';
import { useWebSocket } from '../../context/useWebSocket';
import axiosClient from '../../api/axiosClient';

// 🔴 IMPORT MA THUẬT GIAO DIỆN
import AvatarWithFrame from '../AvatarWithFrame';
import ColoredName from '../ColoredName';
import { showError } from '../../utils/swal';

export default function NotificationBell() {
  const { notifications, unreadCount, markAllAsRead } = useWebSocket();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  
  // State lưu trạng thái xử lý của từng nút (để hiện loading xoay xoay)
  const [processingIds, setProcessingIds] = useState<Set<number>>(new Set());

  // State lưu trạng thái quan hệ bạn bè của các người gửi (để biết nên hiện nút hay ẩn nút)
  // Dạng: { [userId]: 'ACCEPTED' | 'PENDING' | 'NONE' }
  const [friendStatuses, setFriendStatuses] = useState<Record<number, string>>({});
  
  const navigate = useNavigate();
  const open = Boolean(anchorEl);

  // --- HELPER: Trả về icon + màu theo loại thông báo ---
  const getNotifMeta = (type: string): { icon: React.ReactNode; color: string; label: string } => {
    switch (type) {
      case 'REPLY_COMMENT':
        return { icon: <ReplyIcon sx={{ fontSize: 11 }} />, color: '#8b5cf6', label: 'Phản hồi bình luận' };
      case 'COMMENT_POST':
        return { icon: <ChatBubbleIcon sx={{ fontSize: 11 }} />, color: '#3b82f6', label: 'Bình luận bài viết' };
      case 'LIKE_POST':
        return { icon: <FavoriteIcon sx={{ fontSize: 11 }} />, color: '#ef4444', label: 'Thích bài viết' };
      case 'FRIEND_REQUEST':
      case 'ACCEPT_FRIEND':
        return { icon: <PersonAddIcon sx={{ fontSize: 11 }} />, color: '#22c55e', label: type === 'ACCEPT_FRIEND' ? 'Chấp nhận kết bạn' : 'Lời mời kết bạn' };
      case 'SHARE_POST':
        return { icon: <ShareIcon sx={{ fontSize: 11 }} />, color: '#f59e0b', label: 'Chia sẻ bài viết' };
      default:
        return { icon: <NotificationsIcon sx={{ fontSize: 11 }} />, color: '#6b7280', label: 'Thông báo' };
    }
  };

  // --- HELPER: Trả về icon + màu theo kiểu reaction (comment/post reactions) ---
  const getReactionMeta = (reaction: string): { icon: React.ReactNode; color: string; label: string; isImage?: boolean } => {
    // Use react-reactions icons (data-URI images) for consistent reaction visuals
    const key = (reaction || '').toLowerCase();
    const img = icons.find('facebook', key);
    const common = { color: 'primary.main', label: reaction };

    switch (key) {
      // badge is 20x20; make inner icon ~90% (18x18) and use white circular background
      case 'like':
        return { icon: <Box sx={{ width: '95%', height: '95%', backgroundImage: `url(${img})`, backgroundSize: 'contain', backgroundPosition: 'center', backgroundRepeat: 'no-repeat' }} />, color: '#ffffff', label: 'Thích', isImage: true };
      case 'love':
        return { icon: <Box sx={{ width: '95%', height: '95%', backgroundImage: `url(${img})`, backgroundSize: 'contain', backgroundPosition: 'center', backgroundRepeat: 'no-repeat' }} />, color: '#ffffff', label: 'Yêu thích', isImage: true };
      case 'haha':
        return { icon: <Box sx={{ width: '95%', height: '95', backgroundImage: `url(${img})`, backgroundSize: 'contain', backgroundPosition: 'center', backgroundRepeat: 'no-repeat' }} />, color: '#ffffff', label: 'Haha', isImage: true };
      case 'wow':
        return { icon: <Box sx={{ width: '95%', height: '95%', backgroundImage: `url(${img})`, backgroundSize: 'contain', backgroundPosition: 'center', backgroundRepeat: 'no-repeat' }} />, color: '#ffffff', label: 'Wow', isImage: true };
      case 'sad':
        return { icon: <Box sx={{ width: '95%', height: '95%', backgroundImage: `url(${img})`, backgroundSize: 'contain', backgroundPosition: 'center', backgroundRepeat: 'no-repeat' }} />, color: '#ffffff', label: 'Buồn', isImage: true };
      case 'angry':
        return { icon: <Box sx={{ width: '95%', height: '95%', backgroundImage: `url(${img})`, backgroundSize: 'contain', backgroundPosition: 'center', backgroundRepeat: 'no-repeat' }} />, color: '#ffffff', label: 'Giận', isImage: true };
      default:
        return { icon: <InsertEmoticonIcon sx={{ fontSize: 11 }} />, color: '#6b7280', label: 'Phản ứng' };
    }
  };

  // --- 1. XỬ LÝ KHI MỞ MENU THÔNG BÁO ---
  const handleClick = async (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
    
    markAllAsRead();

    // --- LOGIC QUAN TRỌNG: Kiểm tra lại trạng thái bạn bè của các thông báo đang hiển thị ---
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
    e.stopPropagation(); // Chặn click lan ra ngoài
    
    // Đánh dấu đang xử lý để hiện loading
    setProcessingIds(prev => new Set(prev).add(notif.id));

    try {
        if (action === 'accept') {
            await axiosClient.post(`/friends/accept/${notif.senderId}`);
            setFriendStatuses(prev => ({ ...prev, [notif.senderId]: 'ACCEPTED' }));
        } else {
            await axiosClient.delete(`/friends/remove/${notif.senderId}`);
            setFriendStatuses(prev => ({ ...prev, [notif.senderId]: 'DELETED' }));
        }
    } catch (error) {
        console.error("Lỗi xử lý kết bạn:", error);
        showError("Có lỗi xảy ra, vui lòng thử lại.");
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
                
                const showButtons = notif.type === 'FRIEND_REQUEST' && 
                                    (status === undefined || status === 'PENDING');

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
                    
                    {/* 🔴 AVATAR THÔNG BÁO CÓ VIỀN + ICON LOẠI */}
                    <Box sx={{ flexShrink: 0, position: 'relative' }}>
                        <AvatarWithFrame 
                            src={notif.senderAvatar} 
                            name={notif.senderName} 
                            frameClass={notif.senderAvatarFrame}
                            size={48} 
                        />
                        {/* Badge icon nhỏ ở góc dưới phải của avatar */}
                        {(() => {
                          const meta = notif.reactionType ? getReactionMeta(notif.reactionType) : getNotifMeta(notif.type);
                          // Ensure icon renders white on top of colored background
                          let iconElement = meta.icon;
                          if (!meta.isImage && React.isValidElement(meta.icon)) {
                            iconElement = React.cloneElement(meta.icon as React.ReactElement, { sx: { ...( (meta.icon as any).props?.sx || {} ), color: '#fff', fontSize: 11 } });
                          }
                          return (
                            <Tooltip title={meta.label} arrow>
                              <Box sx={{
                                  position: 'absolute',
                                  bottom: -2, right: -2,
                                  width: 20, height: 20,
                                  bgcolor: meta.color,
                                  borderRadius: '50%',
                                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                                  border: '2px solid',
                                  borderColor: 'background.paper',
                                  color: '#fff',
                                  boxShadow: '0 1px 3px rgba(0,0,0,0.3)',
                                  zIndex: 3,
                                }}>
                                {iconElement}
                              </Box>
                            </Tooltip>
                          );
                        })()}
                    </Box>
                    
                    <Box sx={{ flex: 1 }}>
                        <Typography variant="subtitle2" component="div">
                            {/* 🔴 TÊN THÔNG BÁO ĐÃ CÓ MÀU */}
                            <strong>
                                <ColoredName 
                                    name={notif.senderName} 
                                    colorClass={notif.senderNameColor} // Dữ liệu từ Socket Backend
                                />
                            </strong> {notif.message}
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