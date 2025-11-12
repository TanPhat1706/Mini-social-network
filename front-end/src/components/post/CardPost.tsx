import React from 'react';
import {
  Card, CardHeader, CardContent, CardMedia, CardActions,
  Avatar, IconButton, Typography, Box, Divider, Button, Link
} from '@mui/material';
// ⭐️ 1. Import Link của React Router
import { Link as RouterLink } from 'react-router-dom';

// Import Icons
import MoreVertIcon from '@mui/icons-material/MoreVert';
import ThumbUpOutlinedIcon from '@mui/icons-material/ThumbUpOutlined';
import ChatBubbleOutlineOutlinedIcon from '@mui/icons-material/ChatBubbleOutlineOutlined';
import ShareOutlinedIcon from '@mui/icons-material/ShareOutlined';

// 2. DỮ LIỆU MẪU (Mock Data)
const mockPost = {
  user: {
    id: "lehuynhphat", // ⭐️ Thêm ID
    name: "Lê Hồng Phát", 
    avatarUrl: "https://placehold.co/40x40/EFEFEF/333?text=LHP", 
  },
  timestamp: "9 tháng 11 lúc 20:03",
  content: "Lâu lắm mới thay avatar", 
  imageUrl: "https://placehold.co/600x400/CCCCCC/333?text=Post+Image+Here", 
  stats: {
    likes: "33K",
    comments: 628,
    shares: 237,
  },
};
// ----------------------------------

export default function PostCard() {
  return (
    <Card sx={{ maxWidth: '100%', margin: 'auto', mb: 3 }}>
      
      {/* 1. HEADER CỦA BÀI ĐĂNG */}
      <CardHeader
        avatar={
          // ⭐️ 3. Dùng RouterLink bọc Avatar
          // component={RouterLink} biến Link của MUI thành Link của Router
          <Link component={RouterLink} to={`/profile/${mockPost.user.id}`}>
            <Avatar 
              src={mockPost.user.avatarUrl} 
              alt={mockPost.user.name} 
            />
          </Link>
        }
        action={
          <IconButton aria-label="settings">
            <MoreVertIcon />
          </IconButton>
        }
        title={
          // ⭐️ 4. Dùng RouterLink bọc Tên
          <Link 
            component={RouterLink} 
            to={`/profile/${mockPost.user.id}`}
            variant="h6"
            sx={{ 
              fontWeight: 'bold', 
              textDecoration: 'none', 
              color: 'text.primary',
              '&:hover': { textDecoration: 'underline' }
            }}
          >
            {mockPost.user.name}
          </Link>
        }
        subheader={
          <Typography variant="body2" color="text.secondary">
            {mockPost.timestamp}
          </Typography>
        }
      />

      {/* 2. NỘI DUNG TEXT */}
      <CardContent sx={{ pt: 0 }}>
        <Typography variant="body1" color="text.primary">
          {mockPost.content}
        </Typography>
      </CardContent>

      {/* 3. NỘI DUNG HÌNH ẢNH */}
      {mockPost.imageUrl && (
        <CardMedia
          component="img"
          image={mockPost.imageUrl}
          alt="Nội dung bài đăng"
          sx={{ maxHeight: '600px', objectFit: 'contain' }}
        />
      )}

      {/* 4. THANH THỐNG KÊ (STATS) */}
      <Box sx={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center',
        p: 2, 
        pb: 1 
      }}>
        <Typography variant="body2" color="text.secondary">
          👍😂 {mockPost.stats.likes}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {mockPost.stats.comments} bình luận &bull; {mockPost.stats.shares} chia sẻ
        </Typography>
      </Box>

      <Divider variant="middle" />

      {/* 5. CÁC NÚT ACTIONS */}
      <CardActions sx={{ justifyContent: 'space-around', p: 1 }}>
        <Button 
          fullWidth 
          startIcon={<ThumbUpOutlinedIcon />}
          sx={{ color: 'text.secondary' }}
        >
          Thích
        </Button>
        <Button 
          fullWidth 
          startIcon={<ChatBubbleOutlineOutlinedIcon />}
          sx={{ color: 'text.secondary' }}
        >
          Bình luận
        </Button>
        <Button 
          fullWidth 
          startIcon={<ShareOutlinedIcon />}
          sx={{ color: 'text.secondary' }}
        >
          Chia sẻ
        </Button>
      </CardActions>
    </Card>
  );
}