import React, { useEffect, useState } from 'react';
import { Container, Grid, CircularProgress, Box, Typography } from '@mui/material';

// --- IMPORTS COMPONENTS ---
import PostCard, { type PostData } from '../components/post/CardPost';
// import LeftSidebar from '../components/LeftSidebar'; // (Nếu cần thì mở ra)
// import RightSidebar from '../components/RightSidebar'; // (Em đang code cứng sidebar bên phải nên tạm thời chưa dùng cái này)
import CreatePost from '../components/post/CreatePost';
import SuggestedFriends from '../components/friend/SuggestedFriends';
import ChatBox from '../components/chat/ChatBox';

// --- IMPORTS LOGIC & API ---
import api from '../api/api';
import type { User } from '../types';
import { useChat } from '../context/ChatContext';

export default function HomePage() {
  // --- STATE QUẢN LÝ DỮ LIỆU ---
  const [posts, setPosts] = useState<PostData[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  
  // State User & Chat
  const [user, setUser] = useState<User | null>(null); // Thông tin bản thân (để truyền vào ChatBox)
  const [friends, setFriends] = useState<User[]>([]);   // Danh sách người liên hệ (bạn bè)
  
  // Hook Chat
  const { openChat } = useChat(); 

  // Hàm xóa bài viết khỏi list (truyền xuống PostCard)
  const handleRemovePost = (deletedPostId: number) => {
      setPosts(prev => prev.filter(p => p.id !== deletedPostId));
  };

  useEffect(() => {
    // 1. FETCH BÀI VIẾT (FEED)
    const fetchPosts = async () => {
      try {
        setLoading(true);
        const response = await api.get('/api/feed');
        console.log("Fetched feed response:", response);
        setPosts(response.data.content || []); 
      } catch (err) {
        console.error("Failed to fetch posts:", err);
        setError("Không thể tải bảng tin. Vui lòng thử lại sau.");
      } finally {
        setLoading(false);
      }
    };

    // 2. FETCH THÔNG TIN BẢN THÂN (PROFILE)
    // Cần cái này để ChatBox biết ai đang chat
    const fetchProfile = async () => {
        try {
            const res = await api.get('/api/auth/profile'); // Hoặc endpoint tương ứng của em
            setUser(res.data);
        } catch (error) {
            console.error("Lỗi lấy thông tin user:", error);
        }
    };

    // 3. FETCH DANH SÁCH BẠN BÈ (CONTACTS)
    const fetchFriends = async () => {
        try {
            const res = await api.get('/api/auth/friends/list'); // API lấy danh sách bạn bè
            setFriends(res.data);
        } catch (error) {
            console.error("Lỗi lấy danh sách bạn bè:", error);
        }
    };

    // Gọi các hàm
    fetchPosts();
    fetchProfile();
    fetchFriends();
  }, []);

  return (
    <>
      <Container maxWidth="xl" sx={{ mt: 3, px: { xs: 1, md: 5 } }}> 
        <Grid container spacing={2} justifyContent="center">
          
          {/* LEFT SIDEBAR (Ẩn trên mobile) */}
          {/* <Grid item xs={12} md={3} lg={2} sx={{ display: { xs: 'none', md: 'block' }}}>
            <Box sx={{ position: 'sticky', top: 80 }}>
              <LeftSidebar />
            </Box>
          </Grid> */}

          {/* --- CỘT GIỮA: NEWSFEED --- */}
          <Grid item xs={12} md={6} lg={8}>
            <Box sx={{ maxWidth: '800px', mx: 'auto' }}>
            
              {/* CreatePost */}
              <CreatePost />
              
              {/* Danh sách bài viết */}
              {loading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
                  <CircularProgress />
                </Box>
              ) : error ? (
                 <Box sx={{ textAlign: 'center', mt: 4, color: 'error.main' }}><Typography>{error}</Typography></Box>
              ) : posts.length === 0 ? (
                <Box sx={{ textAlign: 'center', mt: 4 }}><Typography color="text.secondary">Chưa có bài đăng nào.</Typography></Box>
              ) : (
                posts.map((post) => (
                  <PostCard key={post.id} post={post} onDeleteSuccess={handleRemovePost} />
                ))
              )}
            </Box>
          </Grid>

          {/* --- CỘT PHẢI: SIDEBAR (GỢI Ý & BẠN BÈ) --- */}
          {/* Lưu ý: Em đang dùng div class sidebar-column, nếu vỡ layout thì bọc nó vào Grid item của MUI nhé */}
          <div className="sidebar-column">
            
            {/* Gợi ý kết bạn */}
            <div className="sidebar-card">
              <SuggestedFriends currentUserId={user?.id || 0} />
            </div>

            {/* DANH SÁCH NGƯỜI LIÊN HỆ (BẠN BÈ) */}
            <div className="sidebar-card" style={{marginTop: '20px'}}>
               <div className="sidebar-title">
                 <span>Người liên hệ</span>
                 <span style={{fontWeight:'normal', fontSize:'13px', color:'#65676b'}}>🔍</span>
               </div>
               
               {friends.length > 0 ? (
                 <div style={{display: 'flex', flexDirection: 'column', gap: '10px'}}>
                   {friends.map(friend => (
                     <div 
                       key={friend.id} 
                       onClick={() => openChat(friend)} // Click để mở chat
                       style={{
                         display: 'flex', alignItems: 'center', gap: '12px', 
                         padding: '8px', borderRadius: '8px', cursor: 'pointer',
                         transition: 'background 0.2s'
                       }}
                       onMouseEnter={(e) => e.currentTarget.style.background = '#f0f2f5'}
                       onMouseLeave={(e) => e.currentTarget.style.background = 'transparent'}
                     >
                       <div style={{position: 'relative'}}>
                         <img 
                           src={friend.avatarUrl || `https://ui-avatars.com/api/?name=${friend.fullName}`} 
                           style={{width: '36px', height: '36px', borderRadius: '50%', objectFit: 'cover'}}
                           alt="ava"
                         />
                         {/* Chấm xanh online */}
                         <div style={{
                           position:'absolute', bottom:0, right:0, 
                           width:'10px', height:'10px', background:'#31a24c', 
                           borderRadius:'50%', border:'2px solid white'
                         }}></div>
                       </div>
                       <div style={{fontWeight: '600', fontSize: '14px', color:'#050505'}}>
                         {friend.fullName}
                       </div>
                     </div>
                   ))}
                 </div>
               ) : (
                 <div style={{color:'#65676b', fontSize:'13px', fontStyle:'italic'}}>
                   Chưa có người liên hệ. Hãy kết bạn thêm nhé!
                 </div>
               )}
            </div>
            
          </div>
        </Grid>
      </Container>

      {/* --- [MỚI] CHATBOX GLOBAL --- */}
      {/* Chỉ hiển thị khi đã load được thông tin user hiện tại */}
      {user && <ChatBox currentUser={user} />}
      
    </>
  );
}