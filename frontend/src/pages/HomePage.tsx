import React, { useEffect, useState } from 'react';
import { Container, Grid, CircularProgress, Box, Typography } from '@mui/material';
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

    const normalized =
      data?.content ||
      data?.data ||
      (Array.isArray(data) ? data : []);

    setFriends(normalized);
  } catch (error) {
    console.error(error);
  }
};

    fetchPosts(); fetchProfile(); fetchFriends();
  }, []);

  return (
    <>
      <Container maxWidth="xl" sx={{ mt: 2 }}> 
        <Grid container spacing={3}>
          {/* CỘT TRÁI - DÙNG USER THẬT */}
          <Grid item xs={0} md={3} sx={{ display: { xs: 'none', md: 'block' } }}>
            <LeftSidebar user={user} />
          </Grid>

          {/* CỘT GIỮA - NEWS FEED */}
          <Grid item xs={12} md={6}>
            <Box sx={{ maxWidth: '680px', mx: 'auto' }}>
              <CreatePost />
              {loading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}><CircularProgress /></Box>
              ) : error ? (
                <Typography sx={{ textAlign: 'center', mt: 4, color: 'error.main' }}>{error}</Typography>
              ) : (
                posts.map((post) => <PostCard key={post.id} post={post} onDeleteSuccess={handleRemovePost} />)
              )}
            </Box>
          </Grid>

          {/* CỘT PHẢI - DÙNG FRIENDS THẬT */}
          <Grid item xs={0} md={3} sx={{ display: { xs: 'none', md: 'block' } }}>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
              <SuggestedFriends currentUserId={user?.id || 0} />
              <RightSidebar friends={friends} onFriendClick={openChat} />
            </Box>
          </Grid>
        </Grid>
      </Container>
      {user && <ChatBox currentUser={user} />}
    </>
  );
}