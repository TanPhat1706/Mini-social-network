import React, { useState, useEffect, useCallback } from 'react';
import {
  Box, Typography, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper,
  Button, Chip, IconButton, Dialog, DialogTitle, DialogContent, TextField, DialogActions, Avatar, Tooltip, CircularProgress, TablePagination, Stack
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import VisibilityIcon from '@mui/icons-material/Visibility';
import PlayCircleOutlineIcon from '@mui/icons-material/PlayCircleOutline';
import FavoriteIcon from '@mui/icons-material/Favorite';
import CommentIcon from '@mui/icons-material/Comment';
import ShareIcon from '@mui/icons-material/Share';

import api from '../../api/api';
import { getApiBaseUrl } from '../../config/apiBase';

// 1. Cập nhật Interface chuẩn xác 100% với JSON từ Backend
interface PostMedia {
  id: number;
  url: string;
  type: string;
}

interface PostAuthor {
  id: number;
  fullName: string;
  avatarUrl?: string | null;
  studentCode?: string;
  // Bổ sung các trường nếu cần hiển thị thêm
}

interface Post {
  id: number;
  content: string;
  visibility: 'PUBLIC' | 'FRIENDS' | 'PRIVATE' | 'PENDING';
  author: PostAuthor;
  media: PostMedia[];
  createdAt: string;
  updatedAt: string;
  likeCount: number;
  commentCount: number;
  shareCount: number;
}

// Interface để hứng cục Page của Spring Boot
interface SpringPageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const PostManager: React.FC = () => {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  // 2. Thêm State quản lý phân trang
  const [page, setPage] = useState<number>(0); // Spring Boot dùng page bắt đầu từ 0
  const [rowsPerPage, setRowsPerPage] = useState<number>(10);
  const [totalElements, setTotalElements] = useState<number>(0);

  const [openDeleteDialog, setOpenDeleteDialog] = useState(false);
  const [selectedPostId, setSelectedPostId] = useState<number | null>(null);
  const [deleteReason, setDeleteReason] = useState('');

  const MEDIA_BASE_URL = getApiBaseUrl();

  const fetchPosts = useCallback(async () => {
    try {
      setLoading(true);

      // 🟢 TỐI ƯU: Sử dụng cấu trúc URL sạch hơn
      // Thêm sort=createdAt,desc để Backend hiểu là lấy bài mới nhất trước
      const params = new URLSearchParams({
        page: page.toString(),
        size: rowsPerPage.toString(),
        sort: 'createdAt,desc' // Sắp xếp theo thời gian tạo, giảm dần
      });

      const response = await api.get<SpringPageResponse<Post>>(`/api/admin/posts?${params.toString()}`);

      setPosts(response.data.content);
      setTotalElements(response.data.totalElements);
    } catch (error) {
      console.error("Lỗi tải bài viết:", error);
      alert("Không thể tải danh sách bài viết!");
    } finally {
      setLoading(false);
    }
  }, [page, rowsPerPage]);

  useEffect(() => {
    fetchPosts();
  }, [fetchPosts]);

  // 4. Xử lý sự kiện phân trang
  const handleChangePage = (event: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0); // Reset về trang đầu khi đổi số lượng hiển thị
  };

  const handleApprove = async (postId: number) => {
    try {
      await api.post(`/api/admin/approve-post/${postId}`);
      fetchPosts();
    } catch (error) {
      console.error(error);
      alert("Lỗi khi duyệt bài.");
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

      // Nếu xóa item cuối cùng của trang, lùi lại 1 trang
      if (posts.length === 1 && page > 0) {
        setPage(page - 1);
      } else {
        fetchPosts();
      }
    } catch (error) {
      console.error(error);
      alert("Lỗi khi xóa bài.");
    }
  };

  // Hàm fomat thời gian chuẩn VN
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleString('vi-VN', {
      hour: '2-digit', minute: '2-digit',
      day: '2-digit', month: '2-digit', year: 'numeric'
    });
  };

  // Giữ nguyên Chiêu Thiên Lý Nhãn
  const renderMediaPreview = (post: Post) => {
    if (!post.media || post.media.length === 0) {
      return <Typography variant="caption" color="text.secondary">Không có</Typography>;
    }

    return (
      <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', maxWidth: 150 }}>
        {post.media.map((item) => {
          let url = item.url;
          if (!url) return null;

          if (url.startsWith('/')) {
            url = `${MEDIA_BASE_URL}${url}`;
          }

          const handlePreviewClick = () => window.open(url, '_blank');

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
                <Avatar variant="rounded" src={url} onClick={handlePreviewClick} sx={{ width: 40, height: 40, border: '1px solid #eee', cursor: 'pointer' }} />
              </Tooltip>
            );
          }
        })}
      </Box>
    );
  };

  return (
    <Box sx={{ p: 2 }}>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 'bold' }}>Quản lý Bài viết</Typography>

      <Paper elevation={2} sx={{ width: '100%', overflow: 'hidden' }}>
        <TableContainer sx={{ maxHeight: '70vh' }}>
          <Table stickyHeader>
            <TableHead>
              <TableRow>
                <TableCell sx={{ bgcolor: '#f5f5f5', fontWeight: 'bold' }}>Tác giả</TableCell>
                <TableCell sx={{ bgcolor: '#f5f5f5', fontWeight: 'bold' }}>Nội dung</TableCell>
                <TableCell sx={{ bgcolor: '#f5f5f5', fontWeight: 'bold' }}>Media</TableCell>
                <TableCell sx={{ bgcolor: '#f5f5f5', fontWeight: 'bold' }}>Thống kê</TableCell>
                <TableCell sx={{ bgcolor: '#f5f5f5', fontWeight: 'bold' }}>Thời gian</TableCell>
                <TableCell sx={{ bgcolor: '#f5f5f5', fontWeight: 'bold' }}>Trạng thái</TableCell>
                <TableCell sx={{ bgcolor: '#f5f5f5', fontWeight: 'bold' }} align="center">Hành động</TableCell>
              </TableRow>
            </TableHead>

            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 5 }}>
                    <CircularProgress />
                  </TableCell>
                </TableRow>
              ) : posts.length > 0 ? posts.map((post) => (
                <TableRow key={post.id} hover>
                  {/* Tác giả */}
                  <TableCell>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                      <Avatar src={post.author.avatarUrl || undefined} sx={{ width: 36, height: 36 }} />
                      <Box>
                        <Typography variant="body2" fontWeight="bold">{post.author.fullName}</Typography>
                        <Typography variant="caption" color="text.secondary">{post.author.studentCode}</Typography>
                      </Box>
                    </Box>
                  </TableCell>

                  {/* Nội dung */}
                  <TableCell sx={{ maxWidth: 200 }}>
                    <Tooltip title={post.content}>
                      <Typography noWrap variant="body2" sx={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {post.content || <span style={{ color: 'gray', fontStyle: 'italic' }}>Không có văn bản</span>}
                      </Typography>
                    </Tooltip>
                  </TableCell>

                  {/* Media */}
                  <TableCell>{renderMediaPreview(post)}</TableCell>

                  {/* Thống kê (Like, Comment, Share) */}
                  <TableCell>
                    <Stack direction="row" spacing={1.5} sx={{ color: 'text.secondary' }}>
                      <Tooltip title={`${post.likeCount} lượt thích`}><Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, fontSize: '0.875rem' }}><FavoriteIcon sx={{ fontSize: 16, color: 'error.main' }} /> {post.likeCount}</Box></Tooltip>
                      <Tooltip title={`${post.commentCount} bình luận`}><Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, fontSize: '0.875rem' }}><CommentIcon sx={{ fontSize: 16, color: 'primary.main' }} /> {post.commentCount}</Box></Tooltip>
                      <Tooltip title={`${post.shareCount} lượt chia sẻ`}><Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, fontSize: '0.875rem' }}><ShareIcon sx={{ fontSize: 16, color: 'success.main' }} /> {post.shareCount}</Box></Tooltip>
                    </Stack>
                  </TableCell>

                  {/* Thời gian */}
                  <TableCell>
                    <Typography variant="body2">{formatDate(post.createdAt)}</Typography>
                  </TableCell>

                  {/* Trạng thái */}
                  <TableCell>
                    <Chip
                      label={post.visibility === 'PENDING' ? 'Chờ duyệt' : post.visibility}
                      color={
                        post.visibility === 'PUBLIC' ? 'success' :
                          post.visibility === 'PENDING' ? 'warning' : 'default'
                      }
                      size="small"
                      sx={{
                        fontWeight: 'bold',
                        // 🟢 HIỆU ỨNG NHẤP NHÁY NHẸ CHO BÀI CHỜ DUYỆT ĐỂ ADMIN CHÚ Ý
                        animation: post.visibility === 'PENDING' ? 'pulse 2s infinite' : 'none',
                        '@keyframes pulse': {
                          '0%': { opacity: 1 },
                          '50%': { opacity: 0.6 },
                          '100%': { opacity: 1 },
                        }
                      }}
                    />
                  </TableCell>

                  {/* Hành động */}
                  <TableCell align="center">
                    <Stack direction="row" justifyContent="center" spacing={0.5}>
                      {post.visibility === 'PENDING' && (
                        <Tooltip title="Duyệt bài">
                          <IconButton size="small" color="success" onClick={() => handleApprove(post.id)}>
                            <CheckCircleIcon />
                          </IconButton>
                        </Tooltip>
                      )}
                      <Tooltip title="Xem chi tiết (Dev sau)">
                        <IconButton size="small" color="info"><VisibilityIcon /></IconButton>
                      </Tooltip>
                      <Tooltip title="Xóa bài">
                        <IconButton size="small" color="error" onClick={() => handleOpenDelete(post.id)}>
                          <DeleteIcon />
                        </IconButton>
                      </Tooltip>
                    </Stack>
                  </TableCell>
                </TableRow>
              )) : (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 5 }}>Chưa có bài viết nào.</TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>

        {/* 5. Phân trang Material UI */}
        <TablePagination
          component="div"
          count={totalElements}
          page={page}
          onPageChange={handleChangePage}
          rowsPerPage={rowsPerPage}
          onRowsPerPageChange={handleChangeRowsPerPage}
          labelRowsPerPage="Số dòng mỗi trang:"
          labelDisplayedRows={({ from, to, count }) => `${from}-${to} trên ${count !== -1 ? count : `hơn ${to}`}`}
        />
      </Paper>

      {/* Dialog Xóa */}
      <Dialog open={openDeleteDialog} onClose={() => setOpenDeleteDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Xác nhận xóa bài viết</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 2 }}>
            Hành động này sẽ xóa vĩnh viễn bài viết khỏi hệ thống. Vui lòng nhập lý do xóa (nếu có).
          </Typography>
          <TextField
            autoFocus margin="dense" label="Lý do xóa" fullWidth variant="outlined"
            value={deleteReason} onChange={(e) => setDeleteReason(e.target.value)}
          />
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setOpenDeleteDialog(false)}>Hủy</Button>
          <Button onClick={handleDeleteConfirm} color="error" variant="contained" disableElevation>Xóa bài viết</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};