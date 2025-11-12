import React from 'react';
import { Container, Grid } from '@mui/material';

import PostCard from '../components/post/CardPost';
import LeftSidebar from '../components/LeftSidebar';
import RightSidebar from '../components/RightSidebar';

// ⭐️ BƯỚC 1: Import component CreatePost
import CreatePost from '../components/post/CreatePost';

export default function HomePage() {
  return (
    <Container maxWidth="lg" sx={{ mt: 3 }}>
      <Grid container spacing={3}>

        {/* CỘT BÊN TRÁI (3/12) */}
        <Grid xs={12} md={3} sx={{ display: { xs: 'none', md: 'block' } }}>
          <LeftSidebar />
        </Grid>

        {/* CỘT Ở GIỮA (6/12) */}
        <Grid xs={12} md={6}>
          
          {/* ⭐️ BƯỚC 2: Thêm component CreatePost vào đây */}
          <CreatePost />
          
          {/* Hiển thị các bài đăng */}
          <PostCard />
          <PostCard />
          <PostCard />
        </Grid>

        {/* CỘT BÊN PHẢI (3/12) */}
        <Grid xs={12} md={3} sx={{ display: { xs: 'none', md: 'block' } }}>
          <RightSidebar />
        </Grid>

      </Grid>
    </Container>
  );
}