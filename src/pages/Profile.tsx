import React, { useEffect, useState } from 'react';
import axios from 'axios';
import axiosClient from '../api/axiosClient';
import api from '../api/api';
import { CircularProgress, Box, Typography } from '@mui/material';

import type { User } from '../types';
import type { UpdateProfileData } from '../types/types';
import type { PostData } from '../components/post/CardPost';

import PostCard from '../components/post/CardPost';
import './Profile.css';

const Profile: React.FC = () => {
  // ===== STATE =====
  const [user, setUser] = useState<User | null>(null);
  const [friends, setFriends] = useState<User[]>([]);
  const [posts, setPosts] = useState<PostData[]>([]);
  const [loadingPosts, setLoadingPosts] = useState(false);

  const [isEditing, setIsEditing] = useState(false);
  const [editFormData, setEditFormData] = useState<UpdateProfileData>({});
  const [previewAvatar, setPreviewAvatar] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  // ===== API =====
  const fetchMyPosts = async () => {
    try {
      setLoadingPosts(true);
      const res = await api.get('/api/posts/my-posts');
      setPosts(res.data.content || []);
    } catch (e) {
      console.error(e);
    } finally {
      setLoadingPosts(false);
    }
  };

  const handleRemovePost = (id: number) => {
    setPosts(prev => prev.filter(p => p.id !== id));
  };

  useEffect(() => {
    const fetchData = async () => {
      const [resUser, resFriends] = await Promise.all([
        axiosClient.get('/profile'),
        axiosClient.get('/friends/list'),
      ]);

      setUser(resUser.data);
      setFriends(resFriends.data);

      setEditFormData({
        fullName: resUser.data.fullName,
        className: resUser.data.className,
        bio: resUser.data.bio,
        avatarUrl: resUser.data.avatarUrl,
      });
    };

    fetchData();
    fetchMyPosts();
  }, []);

  // ===== EDIT =====
  const handleEditChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    setEditFormData({ ...editFormData, [e.target.name]: e.target.value });
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files?.[0]) {
      const file = e.target.files[0];
      setSelectedFile(file);
      setPreviewAvatar(URL.createObjectURL(file));
    }
  };

  const uploadAvatarFile = async (file: File): Promise<string> => {
    const formData = new FormData();
    formData.append('file', file);
    const token = localStorage.getItem('token');

    const res = await axios.post(
      'https://mini-social-network-ayab.onrender.com/api/upload/avatar',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
          Authorization: `Bearer ${token}`,
        },
      }
    );
    return res.data.url;
  };

  const handleSaveProfile = async () => {
    try {
      let avatarUrl = editFormData.avatarUrl;
      if (selectedFile) avatarUrl = await uploadAvatarFile(selectedFile);

      const res = await axiosClient.put('/profile', {
        ...editFormData,
        avatarUrl,
      });

      setUser(res.data);
      setIsEditing(false);
      alert('Cập nhật thành công!');
    } catch (e) {
      alert('Lỗi cập nhật!');
    }
  };

  if (!user) return <div className="loading">Đang tải...</div>;

  return (
    <>
      <div className="profile-page">
        {/* COVER */}
        <div className="cover-section">
          <div className="cover-photo" />
          <div className="profile-header">
            <img
              className="avatar-large"
              src={user.avatarUrl || `https://ui-avatars.com/api/?name=${user.fullName}`}
              alt="avatar"
            />
            <div className="profile-info">
              <h1>{user.fullName}</h1>
              <p>{user.bio || 'Người dùng MiniSocial'}</p>
            </div>
            <button className="edit-btn" onClick={() => setIsEditing(true)}>
              ✏️ Chỉnh sửa
            </button>
          </div>
        </div>

        {/* BODY */}
        <div className="profile-body">
          <div className="left-col">
            <div className="card">
              <h3>Giới thiệu</h3>
              <p>🎓 Lớp: <b>{user.className}</b></p>
              <p>🆔 MSSV: <b>{user.studentCode}</b></p>
              <p>📧 {user.email}</p>
            </div>

            <div className="card">
              <h3>Bạn bè ({friends.length})</h3>
              <div className="friends-grid">
                {friends.slice(0, 6).map(f => (
                  <div key={f.id}>
                    <img src={f.avatarUrl || `https://ui-avatars.com/api/?name=${f.fullName}`} />
                    <span>{f.fullName}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div className="right-col">
            <div className="card title">Bài viết</div>

            {loadingPosts ? (
              <Box sx={{ display: 'flex', justifyContent: 'center' }}>
                <CircularProgress />
              </Box>
            ) : posts.length ? (
              posts.map(p => (
                <PostCard key={p.id} post={p} onDeleteSuccess={handleRemovePost} />
              ))
            ) : (
              <div className="card empty">
                <Typography>Chưa có bài viết</Typography>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* ===== MODAL EDIT ===== */}
      {isEditing && (
        <div className="modal-overlay">
          <div className="modal">
            <div className="modal-header">
              <h2>Chỉnh sửa trang cá nhân</h2>
              <span onClick={() => setIsEditing(false)}>✖</span>
            </div>

            <div className="avatar-edit">
              <img
                src={previewAvatar || editFormData.avatarUrl || ''}
                alt="preview"
              />
              <label htmlFor="file">📷</label>
              <input id="file" type="file" hidden onChange={handleFileChange} />
            </div>

            <div className="form">
              <input
                name="fullName"
                placeholder="Họ và tên"
                value={editFormData.fullName || ''}
                onChange={handleEditChange}
              />
              <input
                name="className"
                placeholder="Lớp"
                value={editFormData.className || ''}
                onChange={handleEditChange}
              />
              <textarea
                name="bio"
                placeholder="Tiểu sử"
                value={editFormData.bio || ''}
                onChange={handleEditChange}
              />
            </div>

            <div className="actions">
              <button onClick={() => setIsEditing(false)}>Hủy</button>
              <button className="primary" onClick={handleSaveProfile}>
                Lưu
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default Profile;
