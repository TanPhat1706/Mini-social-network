import React, { useEffect, useState, useRef, useCallback } from 'react';
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
  const [loadingMore, setLoadingMore] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const [friends, setFriends] = useState<User[]>([]);
  
  // Trạng thái cho Infinite Scroll
  const [nextCursor, setNextCursor] = useState<number | null>(null);
  const [hasNext, setHasNext] = useState<boolean>(true);
  
  const observerTarget = useRef<HTMLDivElement | null>(null);
  const { openChat } = useChat();

  const handleRemovePost = (deletedPostId: number) => {
      setPosts(prev => prev.filter(p => p.id !== deletedPostId));
  };

  const fetchPosts = async (cursor: number | null = null) => {
    if (!hasNext && cursor !== null) return;
    
    try {
      if (cursor === null) setLoading(true);
      else setLoadingMore(true);

      const url = cursor !== null ? `/api/feed?lastPostId=${cursor}&size=10` : `/api/feed?size=10`;
      const response = await api.get(url);
      
      const { content, nextCursor: newCursor, hasNext: moreAvailable } = response.data;
      
      setPosts(prev => cursor === null ? content : [...prev, ...content]);
      setNextCursor(newCursor);
      setHasNext(moreAvailable);
    } catch (err) {
      setError("Không thể tải bảng tin. Vui lòng thử lại sau.");
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  };

  // Logic bắt sự kiện cuộn chuột chạm đáy
  const handleObserver = useCallback((entries: IntersectionObserverEntry[]) => {
    const [target] = entries;
    if (target.isIntersecting && hasNext && !loading && !loadingMore) {
      fetchPosts(nextCursor);
    }
  }, [hasNext, loading, loadingMore, nextCursor]);

  useEffect(() => {
    const observer = new IntersectionObserver(handleObserver, { threshold: 1.0 });
    if (observerTarget.current) observer.observe(observerTarget.current);
    return () => observer.disconnect();
  }, [handleObserver]);

  useEffect(() => {
    const fetchProfileAndFriends = async () => {
        try {
            const [resProfile, resFriends] = await Promise.all([
                api.get('/api/auth/profile'),
                api.get('/api/auth/friends/list')
            ]);
            setUser(resProfile.data);
            const data = resFriends.data;
            setFriends(data?.content || data?.data || (Array.isArray(data) ? data : []));
        } catch (error) { 
            console.error(error); 
        }
    };

    fetchPosts(); 
    fetchProfileAndFriends();
  }, []);

  return (
    <>
      <Box sx={{
        display: 'flex',
        justifyContent: 'center',
        width: '100%',
        mt: 3,
        mb: 5,
        px: { xs: 0, sm: 2, md: 4 }
      }}>
        <Box sx={{
          display: 'flex',
          width: '100%',
          maxWidth: '1600px', 
          justifyContent: 'space-between'
        }}>
          
          {/* CỘT TRÁI */}
          <Box sx={{ 
            width: '320px', flexShrink: 0, display: { xs: 'none', md: 'block' },
            position: 'sticky', top: '80px', height: 'calc(100vh - 80px)', 
            overflowY: 'auto', '&::-webkit-scrollbar': { display: 'none' } 
          }}>
            <LeftSidebar user={user} />
          </Box>

          {/* CỘT GIỮA: NEWS FEED */}
          <Box sx={{ 
            flexGrow: 1, display: 'flex', flexDirection: 'column',
            alignItems: 'center', mx: { xs: 0, md: 3, lg: 5 } 
          }}>
            <Box sx={{ width: '100%', maxWidth: '680px' }}>
              <CreatePost />
              {loading && posts.length === 0 ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}><CircularProgress /></Box>
              ) : error ? (
                <Typography sx={{ textAlign: 'center', mt: 4, color: 'error.main' }}>{error}</Typography>
              ) : (
                <>
                  {posts.map((post) => (
                    <PostCard key={post.id} post={post} onDeleteSuccess={handleRemovePost} />
                  ))}
                  
                  {/* Trạm gác kích hoạt tải thêm bài */}
                  <div ref={observerTarget} style={{ height: '20px', margin: '10px 0' }} />
                  
                  {loadingMore && (
                    <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
                      <CircularProgress size={30} />
                    </Box>
                  )}
                  
                  {!hasNext && posts.length > 0 && (
                    <Typography sx={{ textAlign: 'center', color: 'text.secondary', py: 3 }}>
                      Bạn đã xem hết bài viết.
                    </Typography>
                  )}
                </>
              )}
            </Box>
          </Box>

          {/* CỘT PHẢI */}
          <Box sx={{ 
            width: '320px', flexShrink: 0, display: { xs: 'none', lg: 'block' },
            position: 'sticky', top: '80px', height: 'calc(100vh - 80px)', 
            overflowY: 'auto', '&::-webkit-scrollbar': { display: 'none' } 
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