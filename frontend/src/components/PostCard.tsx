import React from 'react';
import type { Post } from '../types';
import './PostCard.css';

const PostCard: React.FC<{ post: Post }> = ({ post }) => {
  return (
    <div className="post-card">
      <div className="post-header">
        <img src={post.author.avatarUrl} className="post-avatar" alt="ava" />
        <div>
          <div className="post-author">{post.author.fullName}</div>
          <div className="post-time">{new Date(post.createdAt).toLocaleDateString('vi-VN', {hour:'2-digit', minute:'2-digit', day:'numeric', month:'numeric', year: 'numeric'})}</div>
        </div>
      </div>
      <div className="post-content">{post.content}</div>
      {post.imageUrl && <img src={post.imageUrl} style={{width:'100%', display:'block', maxHeight:'500px', objectFit:'cover'}} alt="content" />}
      
      <div className="post-actions">
        <div className="action-btn"><span>👍</span> Thích ({post.likesCount})</div>
        <div className="action-btn"><span>💬</span> Bình luận</div>
        <div className="action-btn"><span>↪</span> Chia sẻ</div>
      </div>
    </div>
  );
};
export default PostCard;