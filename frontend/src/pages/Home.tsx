import React, { useState, useEffect } from 'react';
import type { Post, User } from '../types';
import { mockPosts } from '../data/mockPosts';
import Header from '../components/Header';
import Footer from '../components/Footer';
import PostCard from '../components/PostCard';
import SuggestedFriends from '../components/SuggestedFriends';
import ChatBox from '../components/ChatBox'; // <--- IMPORT CHATBOX
import axiosClient from '../api/axiosClient';
import { useAuth } from '../context/AuthContext';
import { useChat } from '../context/ChatContext'; // <--- IMPORT
import './Home.css';

const Home: React.FC = () => {
  const [posts, setPosts] = useState<Post[]>(mockPosts);
  const [user, setUser] = useState<User | null>(null);
  const [content, setContent] = useState('');
  
  // --- STATE MỚI CHO CHAT ---
  const [friends, setFriends] = useState<User[]>([]); // Danh sách người liên hệ
  const [chatTarget, setChatTarget] = useState<User | null>(null); // Người đang chat cùng
  // --------------------------

  const { logout } = useAuth();
  const { openChat } = useChat(); // <--- DÙNG HOOK

  useEffect(() => {
    // 1. Lấy thông tin bản thân
    axiosClient.get('/profile').then(res => setUser(res.data)).catch(logout);

    // 2. Lấy danh sách bạn bè (Người liên hệ)
    axiosClient.get('/friends/list')
      .then(res => setFriends(res.data))
      .catch(err => console.error(err));
      
  }, [logout]);

  const handlePost = () => {
    if (!content.trim() || !user) return;
    const newPost: Post = {
      id: Date.now(),
      author: { id: user.id, fullName: user.fullName, avatarUrl: user.avatarUrl || '' },
      content, createdAt: new Date().toISOString(), likesCount: 0, commentsCount: 0
    };
    setPosts([newPost, ...posts]); setContent('');
  };

  return (
    <>
      <Header />
      
      <div className="home-wrapper" style={{ minHeight: 'calc(100vh - 150px)' }}> 
        {/* --- CỘT GIỮA: FEED --- */}
        <div className="feed-column">
          <div className="create-post-box">
            <div className="cp-top">
              <img src={user?.avatarUrl || "https://ui-avatars.com/api/?background=random"} className="cp-avatar" alt="me"/>
              <input 
                className="cp-input" 
                placeholder={`Ơi ${user?.fullName}, bạn đang nghĩ gì thế?`} 
                value={content} 
                onChange={e => setContent(e.target.value)} 
                onKeyDown={e => e.key === 'Enter' && handlePost()}
              />
            </div>
            {content.trim() && <button className="cp-btn" onClick={handlePost}>Đăng bài viết</button>}
          </div>
          {posts.map(p => <PostCard key={p.id} post={p} />)}
        </div>

        {/* --- CỘT PHẢI: SIDEBAR --- */}
        <div className="sidebar-column">
          
          <div className="sidebar-card">
            <SuggestedFriends currentUserId={user?.id || 0} />
          </div>

          {/* --- [MỚI] DANH SÁCH NGƯỜI LIÊN HỆ (BẠN BÈ) --- */}
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
                      onClick={() => openChat(friend)} // <--- GỌI OPEN CHAT TỪ CONTEXT
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
                        {/* Chấm xanh online giả lập */}
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
          {/* ----------------------------------------------- */}

        </div>
      </div>

      <Footer />

      {/* CHATBOX GLOBAL: Tự động hiển thị khi Context có targetUser */}
      {user && <ChatBox currentUser={user} />}
      
    </>
  );
};

export default Home;