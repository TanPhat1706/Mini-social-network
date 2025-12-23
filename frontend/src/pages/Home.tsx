import React, { useState, useEffect } from 'react';
import type { Post, User } from '../types';
import { mockPosts } from '../data/mockPosts';
import Header from '../components/Header';
import Footer from '../components/Footer'; // Import Footer mới
import PostCard from '../components/PostCard';
import SuggestedFriends from '../components/SuggestedFriends';
import axiosClient from '../api/axiosClient';
import { useAuth } from '../context/AuthContext';
import './Home.css';

const Home: React.FC = () => {
  const [posts, setPosts] = useState<Post[]>(mockPosts);
  const [user, setUser] = useState<User | null>(null);
  const [content, setContent] = useState('');
  const { logout } = useAuth();

  useEffect(() => {
    axiosClient.get('/profile').then(res => setUser(res.data)).catch(logout);
  }, []);

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
      <div className="home-wrapper">
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

        <div className="sidebar-column">
          {/* Đã xóa FriendRequests ở đây vì nó đã chuyển lên Header */}

          <div className="sidebar-card">
            <SuggestedFriends currentUserId={user?.id || 0} />
          </div>

        </div>
      </div>
      <Footer />
    </>
  );
};
export default Home;