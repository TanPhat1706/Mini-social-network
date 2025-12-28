import React from 'react';
import { Container, Grid, Box } from '@mui/material';

// Import các component con
import ProfileHeader from '../components/profile/ProfileHeader';
import ProfileIntro from '../components/profile/ProfileIntro';
import CreatePost from '../components/post/CreatePost';

// Đảm bảo đường dẫn này trỏ đến file CardPost.tsx của bạn
import PostCard from '../components/post/CardPost'; 

export default function ProfilePage() {
  return (
    <Box sx={{ bgcolor: 'background.default' }}>
      {/* 1. PHẦN HEADER (Bìa, Avatar, Navbar) */}
      <ProfileHeader />

      {/* 2. PHẦN THÂN (Bố cục 2 cột) */}
      <Container maxWidth="md" sx={{ mt: 3 }}>
        <Grid container spacing={3}>
          
          {/* 2a. CỘT BÊN TRÁI (Giới thiệu) */}
          {/* ⭐️ SỬA LỖI: Đổi 'md' (900px) thành 'sm' (600px) */}
          <Grid item xs={12} sm={5}>
            <ProfileIntro />
          </Grid>

          {/* 2b. CỘT BÊN PHẢI (Bài viết) */}
          {/* ⭐️ SỬA LỖI: Đổi 'md' (900px) thành 'sm' (600px) */}
          <Grid item xs={12} sm={7}>
            {/* Gọi lại CreatePost (đã loại bỏ thanh filter) */}
            <CreatePost />
            
            {/* Danh sách các bài viết */}
            <PostCard />
            <PostCard />
          </Grid>
          
        </Grid>
      </Container>
    </Box>
  );
}