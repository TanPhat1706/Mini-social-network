import React, { useEffect, useState } from 'react';
import { Box, CircularProgress, Typography } from '@mui/material';
import PostCard, { type PostData } from '../components/post/CardPost';
import LeftSidebar from '../components/LeftSidebar'; 
import RightSidebar from '../components/RightSidebar'; 
import CreatePost from '../components/post/CreatePost';
import SuggestedFriends from '../components/friend/SuggestedFriends';
import ChatBox from '../components/chat/ChatBox';
import api from '../api/api';
import type { User } from '../types';
import { useChat } from '../context/ChatContext';

export default function HomePage() {
  const [posts, setPosts] = useState<PostData[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const [friends, setFriends] = useState<User[]>([]);
  
  const { openChat } = useChat();

  const handleRemovePost = (deletedPostId: number) => {
      setPosts(prev => prev.filter(p => p.id !== deletedPostId));
  };

  useEffect(() => {
    const fetchPosts = async () => {
      try {
        setLoading(true);
        const response = await api.get('/api/feed');
        setPosts(response.data.content || []); 
      } catch (err) {
        setError("Không thể tải bảng tin. Vui lòng thử lại sau.");
      } finally {
        setLoading(false);
      }
    };

    const fetchProfile = async () => {
        try {
            const res = await api.get('/api/auth/profile');
            setUser(res.data);
        } catch (error) { console.error(error); }
    };

    const fetchFriends = async () => {
      try {
        const res = await api.get('/api/auth/friends/list');
        const data = res.data;
        const normalized = data?.content || data?.data || (Array.isArray(data) ? data : []);
        setFriends(normalized);
      } catch (error) {
        console.error(error);
      }
    };

    fetchPosts(); fetchProfile(); fetchFriends();
  }, []);

  return (
    <>
      {/* 🔴 VỨT BỎ <Container> và <Grid> - DÙNG FLEXBOX BAO PHỦ 100% */}
      <Box sx={{
        display: 'flex',
        justifyContent: 'center',
        width: '100%',
        mt: 3,
        mb: 5,
        px: { xs: 0, sm: 2, md: 4 }
      }}>
        
        {/* Khung chứa 3 cột, nới rộng tối đa lên 1600px */}
        <Box sx={{
          display: 'flex',
          width: '100%',
          maxWidth: '1600px', 
          justifyContent: 'space-between'
        }}>
          
          {/* --- CỘT TRÁI (Cố định 320px) --- */}
          <Box sx={{ 
            width: '320px', 
            flexShrink: 0, 
            display: { xs: 'none', md: 'block' },
            position: 'sticky', 
            top: '80px', 
            height: 'calc(100vh - 80px)', 
            overflowY: 'auto', 
            '&::-webkit-scrollbar': { display: 'none' } 
          }}>
            <LeftSidebar user={user} />
          </Box>

          {/* --- CỘT GIỮA: NEWS FEED (Cởi trói giới hạn) --- */}
          <Box sx={{ 
            flexGrow: 1, // Chiếm toàn bộ khoảng trống ở giữa
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center', // Ép các bài viết nằm ngay giữa màn hình
            mx: { xs: 0, md: 3, lg: 5 } // Tạo khoảng cách thở với 2 cột bên
          }}>
            {/* Kích thước Vàng của bài viết FB là 680px */}
            <Box sx={{ width: '100%', maxWidth: '680px' }}>
              <CreatePost />
              {loading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}><CircularProgress /></Box>
              ) : error ? (
                <Typography sx={{ textAlign: 'center', mt: 4, color: 'error.main' }}>{error}</Typography>
              ) : (
                posts.map((post) => <PostCard key={post.id} post={post} onDeleteSuccess={handleRemovePost} />)
              )}
            </Box>
          </Box>

          {/* --- CỘT PHẢI (Cố định 320px) --- */}
          <Box sx={{ 
            width: '320px', 
            flexShrink: 0, 
            display: { xs: 'none', lg: 'block' },
            position: 'sticky', 
            top: '80px', 
            height: 'calc(100vh - 80px)', 
            overflowY: 'auto', 
            '&::-webkit-scrollbar': { display: 'none' } 
          }}>
            <SuggestedFriends currentUserId={user?.id || 0} />
            <RightSidebar friends={friends} onFriendClick={openChat} />
          </Box>

        </Box>
      </Box>

      {/* {user && <ChatBox currentUser={user} />} */}
    </>
  );
}