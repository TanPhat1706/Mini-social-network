import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Container, Box, CircularProgress, Typography, Button } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import api from '../api/api'; // Axios instance của em
import PostCard from '../components/post/CardPost';
import type { PostData } from '../components/post/CardPost';

export default function PostDetailPage() {
  const { postId } = useParams<{ postId: string }>(); // Lấy ID từ URL
  const navigate = useNavigate();

  const [post, setPost] = useState<PostData | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchPostDetail = async () => {
      try {
        setLoading(true);
        // Gọi API lấy chi tiết bài viết (Backend cần có API này)
        const response = await api.get(`/api/posts/${postId}`);
        setPost(response.data);
      } catch (err: any) {
        console.error("Lỗi tải bài viết:", err);
        setError("Bài viết không tồn tại hoặc đã bị xóa.");
      } finally {
        setLoading(false);
      }
    };

    if (postId) {
      fetchPostDetail();
    }
  }, [postId]);

  // Xử lý khi xóa bài viết thành công ngay tại trang chi tiết
  const handleDeleteSuccess = (deletedId: number) => {
    alert("Đã xóa bài viết.");
    navigate('/'); // Xóa xong thì quay về trang chủ
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 10 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error || !post) {
    return (
      <Container maxWidth="md" sx={{ mt: 5, textAlign: 'center' }}>
        <Typography variant="h5" color="error" gutterBottom>
          {error || "Không tìm thấy bài viết"}
        </Typography>
        <Button variant="outlined" startIcon={<ArrowBackIcon />} onClick={() => navigate('/')}>
          Quay về trang chủ
        </Button>
      </Container>
    );
  }

  return (
    <Container maxWidth="md" sx={{ mt: 3, pb: 5 }}>
      {/* Nút quay lại cho tiện */}
      <Button 
        startIcon={<ArrowBackIcon />} 
        onClick={() => navigate(-1)} 
        sx={{ mb: 2 }}
      >
        Quay lại
      </Button>

      {/* Render lại PostCard y hệt trang chủ */}
      <PostCard 
        post={post} 
        onDeleteSuccess={handleDeleteSuccess} 
      />
    </Container>
  );
}