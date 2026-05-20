import React, { useState, useEffect } from 'react';
import { 
  Box, Typography, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, 
  Button, Chip, IconButton, Dialog, DialogTitle, DialogContent, TextField, DialogActions, Avatar, Tooltip, CircularProgress
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import VisibilityIcon from '@mui/icons-material/Visibility';
import PlayCircleOutlineIcon from '@mui/icons-material/PlayCircleOutline';
import BrokenImageIcon from '@mui/icons-material/BrokenImage';

import api from '../../api/api';
import { getApiBaseUrl } from '../../config/apiBase';

interface PostMedia {
  id: number;
  url: string;
  type: string;
}

interface PostAuthor {
  id: number;
  fullName: string;
  avatarUrl?: string;
  studentCode?: string;
}

interface Post {
  id: number;
  content: string;
  visibility: 'PUBLIC' | 'FRIENDS' | 'PRIVATE' | 'PENDING';
  author: PostAuthor;
  media: PostMedia[];
  createdAt: string;
  likeCount: number;
  commentCount: number;
}

export const PostManager: React.FC = () => {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  
  const [openDeleteDialog, setOpenDeleteDialog] = useState(false);
  const [selectedPostId, setSelectedPostId] = useState<number | null>(null);
  const [deleteReason, setDeleteReason] = useState('');

  const MEDIA_BASE_URL = getApiBaseUrl();

  useEffect(() => {
    fetchPosts();
  }, []);

  const fetchPosts = async () => {
    try {
      setLoading(true);
      const response = await api.get('/api/admin/posts');
      setPosts(response.data);
    } catch (error) {
      console.error("Lỗi tải bài viết:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (postId: number) => {
    try {
      await api.post(`/api/admin/approve-post/${postId}`);
      fetchPosts(); 
    } catch (error) {
      showError("Lỗi khi duyệt bài.");
    }
  };

  const handleOpenDelete = (postId: number) => {
    setSelectedPostId(postId);
    setDeleteReason('');
    setOpenDeleteDialog(true);
  };

  const handleDeleteConfirm = async () => {
    if (!selectedPostId) return;
    try {
      await api.delete(`/api/admin/delete-post/${selectedPostId}`, {
        params: { reason: deleteReason }
      });
      setOpenDeleteDialog(false);
      fetchPosts();
    } catch (error) {
      showError("Lỗi khi xóa bài.");
    }
  };

  // ⭐️ CHIÊU 2: THIÊN LÝ NHÃN (Hiển thị toàn bộ Media)
  const renderMediaPreview = (post: Post) => {
    if (!post.media || post.media.length === 0) {
      return <Typography variant="caption" color="text.secondary">Không có</Typography>;
    }

    return (
      <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', maxWidth: 300 }}>
        {post.media.map((item) => {
          let url = item.url;
          if (!url) return null;
          
          if (url.startsWith('/')) {
            url = `${MEDIA_BASE_URL}${url}`;
          }

          const handlePreviewClick = () => {
             window.open(url, '_blank');
          };

          if (item.type === 'VIDEO') {
             return (
               <Tooltip key={item.id} title="Xem video">
                 <Box onClick={handlePreviewClick} sx={{ width: 40, height: 40, borderRadius: 1, bgcolor: '#000', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', border: '1px solid #ccc' }}>
                    <PlayCircleOutlineIcon sx={{ color: 'white', fontSize: 20 }} />
                 </Box>
               </Tooltip>
             );
          } else {
             return (
               <Tooltip key={item.id} title="Xem ảnh">
                  <Avatar 
                    variant="rounded" 
                    src={url} 
                    onClick={handlePreviewClick} 
                    sx={{ width: 40, height: 40, border: '1px solid #eee', cursor: 'pointer' }} 
                  />
               </Tooltip>
             );
          }
        })}
      </Box>
    );
  };

  if (loading) return <Box sx={{ p: 3, textAlign: 'center' }}><CircularProgress /></Box>;

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 'bold' }}>Quản lý Bài viết</Typography>

      <TableContainer component={Paper} elevation={2}>
        <Table>
          <TableHead sx={{ bgcolor: '#f5f5f5' }}>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Tác giả</TableCell>
              <TableCell>Nội dung</TableCell>
              <TableCell>Media</TableCell>
              <TableCell>Trạng thái</TableCell>
              <TableCell align="right">Hành động</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {posts.length > 0 ? posts.map((post) => (
              <TableRow key={post.id} hover>
                <TableCell>{post.id}</TableCell>
                <TableCell>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Avatar src={post.author.avatarUrl} sx={{ width: 30, height: 30 }} />
                    <Box>
                        <Typography variant="body2" fontWeight="bold">{post.author.fullName}</Typography>
                        <Typography variant="caption">{post.author.studentCode}</Typography>
                    </Box>
                  </Box>
                </TableCell>
                <TableCell sx={{ maxWidth: 250 }}>
                   <Tooltip title={post.content}>
                     <Typography noWrap variant="body2" sx={{overflow: 'hidden', textOverflow: 'ellipsis'}}>{post.content}</Typography>
                   </Tooltip>
                </TableCell>
                {/* Hiển thị list ảnh */}
                <TableCell>{renderMediaPreview(post)}</TableCell>
                <TableCell>
                  <Chip 
                    label={post.visibility} 
                    color={post.visibility === 'PUBLIC' ? 'success' : post.visibility === 'PENDING' ? 'warning' : 'default'} 
                    size="small" 
                    sx={{ fontWeight: 'bold' }}
                  />
                </TableCell>
                <TableCell align="right">
                  <Tooltip title="Xem chi tiết">
                    <IconButton size="small" color="info"><VisibilityIcon /></IconButton>
                  </Tooltip>
                  
                  {post.visibility === 'PENDING' && (
                    <Tooltip title="Duyệt bài">
                      <IconButton size="small" color="success" onClick={() => handleApprove(post.id)}>
                          <CheckCircleIcon />
                      </IconButton>
                    </Tooltip>
                  )}

                  <Tooltip title="Xóa bài">
                    <IconButton size="small" color="error" onClick={() => handleOpenDelete(post.id)}>
                        <DeleteIcon />
                    </IconButton>
                  </Tooltip>
                </TableCell>
              </TableRow>
            )) : (
              <TableRow>
                <TableCell colSpan={6} align="center" sx={{ py: 3 }}>Chưa có bài viết nào.</TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={openDeleteDialog} onClose={() => setOpenDeleteDialog(false)}>
        <DialogTitle>Xác nhận xóa bài viết</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 2 }}>
            Hành động này không thể hoàn tác.
          </Typography>
          <TextField
            autoFocus margin="dense" label="Lý do xóa" fullWidth variant="outlined"
            value={deleteReason} onChange={(e) => setDeleteReason(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDeleteDialog(false)}>Hủy</Button>
          <Button onClick={handleDeleteConfirm} color="error" variant="contained">Xóa ngay</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};