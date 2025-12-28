import React, { useState } from 'react';
import {
  Card, CardHeader, CardContent, CardActions,
  Avatar, IconButton, Typography, Box, Divider, Button, Link,
  Menu, MenuItem, ListItemIcon,
  Collapse
} from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';

// Import Icons
import MoreVertIcon from '@mui/icons-material/MoreVert';
import ThumbUpOutlinedIcon from '@mui/icons-material/ThumbUpOutlined';
import ThumbUpIcon from '@mui/icons-material/ThumbUp';
import ChatBubbleOutlineOutlinedIcon from '@mui/icons-material/ChatBubbleOutlineOutlined';
import ShareOutlinedIcon from '@mui/icons-material/ShareOutlined';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';

// Import API & Components
import api from '../../api/api';
import EditPost from './EditPost';
import PostMediaGrid from './PostMediaGrid';
import CommentSection from '../comment/CommentSection';

// --- Types ---
export interface PostMedia {
  id: number;
  url: string;
  type: string;
}

export interface PostAuthor {
  id: number;
  username: string;
  fullName: string;
  avatarUrl: string;
}

export interface PostData {
  id: number;
  content: string;
  createdAt: string;
  author: PostAuthor;
  media: PostMedia[];
  likeCount: number;
  commentCount: number;
  shareCount: number;
  originalPost?: PostData;
  likedByCurrentUser: boolean;
  visibility?: string;
}

interface PostCardProps {
  post: PostData;
  onDeleteSuccess: (id: number) => void;
}

const formatDate = (dateString: string) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return new Intl.DateTimeFormat('vi-VN', { 
    day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit' 
  }).format(date);
};

// ⭐️ COMPONENT CON: HIỂN THỊ NỘI DUNG BÀI GỐC (LỒNG BÊN TRONG)
const SharedPostContent = ({ originalPost }: { originalPost: PostData }) => {
    // Trường hợp bài gốc đã bị xóa
    if (!originalPost) {
      return (
        <Box sx={{ p: 2, bgcolor: '#F0F2F5', border: '1px solid #ddd', borderRadius: 2 }}>
          <Typography variant="body2" color="text.secondary" fontStyle="italic">
            Bài viết này không còn khả dụng.
          </Typography>
        </Box>
      );
    }
  
    return (
      <Box sx={{ mt: 2, border: '1px solid #ddd', borderRadius: 2, overflow: 'hidden' }}>
          {/* Header nhỏ của bài gốc */}
          <Box sx={{ p: 1.5, display: 'flex', alignItems: 'center', bgcolor: '#F7F8FA', borderBottom: '1px solid #eee' }}>
              <Avatar 
                  src={originalPost.author.avatarUrl} 
                  sx={{ width: 32, height: 32, mr: 1.5 }} 
              />
              <Box>
                  <Link component={RouterLink} to={`/profile/${originalPost.author.id}`} underline="hover" color="text.primary">
                    <Typography variant="subtitle2" fontWeight="bold">
                        {originalPost.author.fullName}
                    </Typography>
                  </Link>
                  <Link component={RouterLink} to={`/posts/${originalPost.id}`} underline="hover" color="text.secondary">
                    <Typography variant="caption">
                        {formatDate(originalPost.createdAt)}
                    </Typography>
                  </Link>
              </Box>
          </Box>
  
          {/* Nội dung text bài gốc */}
          <Box sx={{ px: 2, py: 1 }}>
               <Typography variant="body2" style={{ whiteSpace: 'pre-line' }}>{originalPost.content}</Typography>
          </Box>
  
          {/* Media bài gốc */}
          {originalPost.media && originalPost.media.length > 0 && (
              <PostMediaGrid media={originalPost.media} />
          )}
      </Box>
    );
  };

// ⭐️ MAIN COMPONENT
export default function PostCard({ post: initialPost, onDeleteSuccess }: PostCardProps) {
  
  const [post, setPost] = useState<PostData>(initialPost);

  // MENU & DIALOG STATE
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [showComments, setShowComments] = useState(false);
  
  const isMenuOpen = Boolean(anchorEl);
  // const isOwner = true; // Logic check owner của em

  // --- HANDLERS ---
  const handleMenuClick = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleEditClick = () => {
    setIsEditDialogOpen(true);
    handleMenuClose();
  };

  const handleLikeClick = async () => {
    const previousPostState = { ...post };
    const isCurrentlyLiked = post.likedByCurrentUser;
    
    setPost(prev => ({
        ...prev,
        likedByCurrentUser: !isCurrentlyLiked, 
        likeCount: isCurrentlyLiked ? prev.likeCount - 1 : prev.likeCount + 1 
    }));

    try {
        await api.post(`/api/posts/${post.id}/like`);
    } catch (error) {
        console.error("Lỗi like:", error);
        setPost(previousPostState);
    }
  };

  const handleDeleteClick = async () => {
    if (window.confirm("Bạn có chắc chắn muốn xóa bài viết này không?")) {
        try {
            await api.delete(`/api/posts/${post.id}`);
            onDeleteSuccess(post.id); 
        } catch (error) {
            console.error("Error deleting post:", error);
            alert("Lỗi khi xóa bài viết!");
        }
    }
    handleMenuClose();
  };

  const handleUpdateSuccess = (updatedPost: PostData) => {
      setPost(updatedPost);
  };

  const handleCommentClick = () => {
    setShowComments(!showComments);
  };

  // ⭐️ XỬ LÝ SHARE (ĐƠN GIẢN HÓA ĐỂ CHẠY ĐƯỢC)
  const handleShareClick = async () => {
      const caption = prompt("Nhập nội dung bạn muốn chia sẻ:");
      if (caption === null) return; // Người dùng bấm Cancel

      try {
          // Gọi API Share (Backend em đã làm ở bước trước)
          // Endpoint: /api/posts/{postId}/share
          await api.post(`/api/posts/${post.id}/share`, { content: caption });
          alert("Chia sẻ thành công!");
          // Tăng số lượng share ảo để UI phản hồi
          setPost(prev => ({ ...prev, shareCount: (prev.shareCount || 0) + 1 }));
      } catch (error) {
          console.error("Lỗi share:", error);
          alert("Không thể chia sẻ bài viết này.");
      }
  };

  return (
    <>
      <Card sx={{ maxWidth: '100%', margin: 'auto', mb: 3, boxShadow: 3, borderRadius: 2 }}>
        
        {/* HEADER: Luôn hiển thị người đăng bài hiện tại (Tác giả hoặc Người Share) */}
        <CardHeader
          avatar={
            <Link component={RouterLink} to={`/profile/${post.author.id}`}>
              <Avatar src={post.author.avatarUrl} alt={post.author.fullName} />
            </Link>
          }
          action={
              <IconButton aria-label="settings" onClick={handleMenuClick}>
                <MoreVertIcon />
              </IconButton>
          }
          title={
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                <Link component={RouterLink} to={`/profile/${post.author.id}`}
                variant="h6"
                sx={{ fontWeight: 'bold', textDecoration: 'none', color: 'text.primary', '&:hover': { textDecoration: 'underline' }}}
                >
                {post.author.fullName}
                </Link>
                {/* Nếu là bài share thì thêm dòng chữ nhỏ */}
                {post.originalPost && (
                    <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 'normal' }}>
                        đã chia sẻ một bài viết
                    </Typography>
                )}
            </Box>
          }
          subheader={
            <Link 
              component={RouterLink} 
              to={`/posts/${post.id}`} 
              color="inherit"
              underline="hover"
              sx={{ color: 'text.secondary', fontSize: '0.875rem' }}
            >
              {formatDate(post.createdAt)}
            </Link>
          }
        />

        <Menu
          anchorEl={anchorEl}
          open={isMenuOpen}
          onClose={handleMenuClose}
        >
          <MenuItem onClick={handleEditClick}>
            <ListItemIcon><EditIcon fontSize="small" /></ListItemIcon>
            Chỉnh sửa bài viết
          </MenuItem>
          <MenuItem onClick={handleDeleteClick} sx={{ color: 'error.main' }}>
            <ListItemIcon><DeleteIcon fontSize="small" color="error" /></ListItemIcon>
            Xóa bài viết
          </MenuItem>
        </Menu>

        <CardContent sx={{ pt: 0 }}>
          {/* 1. CONTENT CỦA NGƯỜI ĐĂNG (Caption) */}
          {post.content && (
            <Typography variant="body1" color="text.primary" style={{ whiteSpace: 'pre-line', marginBottom: post.originalPost ? 16 : 0 }}>
                {post.content}
            </Typography>
          )}

          {/* 2. LOGIC PHÂN CHIA: HIỂN THỊ MEDIA HAY BÀI SHARE */}
          {post.originalPost ? (
              // 
              // NẾU LÀ BÀI SHARE -> Render Component con lồng nhau
              <SharedPostContent originalPost={post.originalPost} />
          ) : (
              // NẾU LÀ BÀI GỐC -> Render Media bình thường
              post.media && post.media.length > 0 && (
                  <Box sx={{ mt: 1 }}>
                    <PostMediaGrid media={post.media} />
                  </Box>
              )
          )}
        </CardContent>

        {/* STATS */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', px: 2, py: 1 }}>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
             <ThumbUpIcon sx={{ width: 16, height: 16, color: 'primary.main', mr: 0.5 }} />
             <Typography variant="body2" color="text.secondary">{post.likeCount}</Typography>
          </Box>
          
          <Box sx={{ display: 'flex', gap: 2 }}>
            <Typography 
                variant="body2" color="text.secondary" 
                sx={{ cursor: 'pointer', '&:hover': { textDecoration: 'underline' } }}
                onClick={handleCommentClick}
            >
                {post.commentCount} bình luận
            </Typography>
            <Typography variant="body2" color="text.secondary">
                {post.shareCount || 0} chia sẻ
            </Typography>
          </Box>
        </Box>

        <Divider variant="middle" sx={{ my: 0 }} />

        {/* ACTIONS */}
        <CardActions sx={{ justifyContent: 'space-around', p: 1 }}>
          <Button 
            fullWidth 
            onClick={handleLikeClick}
            startIcon={post.likedByCurrentUser ? <ThumbUpIcon /> : <ThumbUpOutlinedIcon />} 
            sx={{ 
                color: post.likedByCurrentUser ? 'primary.main' : 'text.secondary',
                fontWeight: post.likedByCurrentUser ? 'bold' : 'normal',
                textTransform: 'none'
            }}
          >
            Thích
          </Button>

          <Button 
            fullWidth 
            onClick={handleCommentClick}
            startIcon={<ChatBubbleOutlineOutlinedIcon />} 
            sx={{ color: 'text.secondary', textTransform: 'none' }}
          >
            Bình luận
          </Button>
          
          {/* ⭐️ NÚT SHARE */}
          <Button 
            fullWidth 
            onClick={handleShareClick}
            startIcon={<ShareOutlinedIcon />} 
            sx={{ color: 'text.secondary', textTransform: 'none' }}
          >
            Chia sẻ
          </Button>
        </CardActions>

        <Collapse in={showComments} timeout="auto" unmountOnExit>
            <Divider />
            <Box sx={{ p: 2 }}>
                <CommentSection 
                    postId={post.id} 
                    currentUserAvatar="https://via.placeholder.com/150" 
                />
            </Box>
        </Collapse>
      </Card>

      {isEditDialogOpen && (
        <EditPost
          open={isEditDialogOpen}
          onClose={() => setIsEditDialogOpen(false)}
          post={post}
          onUpdateSuccess={handleUpdateSuccess}
        />
      )}
    </>
  );
}