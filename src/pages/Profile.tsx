import React, { useEffect, useState } from 'react';
import Header from '../components/layout/Header';
import axiosClient from '../api/axiosClient';
import axios from 'axios'; 
import { CircularProgress, Box, Typography } from '@mui/material';

// Import Types & Components
import type { User, UpdateProfileData } from '../types';
import PostCard from '../components/post/CardPost';
import type { PostData } from '../components/post/CardPost';
import FriendButton from '../components/friend/FriendButton';
import './Profile.css';
import api from '../api/api';

const Profile: React.FC = () => {
  // --- STATE QUẢN LÝ DỮ LIỆU ---
  const [user, setUser] = useState<User | null>(null);
  const [friends, setFriends] = useState<User[]>([]);
  const [posts, setPosts] = useState<PostData[]>([]);
  const [loadingPosts, setLoadingPosts] = useState(false);

  // --- STATE EDIT PROFILE ---
  const [isEditing, setIsEditing] = useState(false);
  const [editFormData, setEditFormData] = useState<UpdateProfileData>({});
  const [previewAvatar, setPreviewAvatar] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  // --- 1. CÁC HÀM API ---
  const fetchMyPosts = async () => {
    try {
      setLoadingPosts(true);
      // Gọi API lấy bài viết của chính mình (Backend: PostController)
      const res = await api.get('/api/posts/my-posts'); 
      setPosts(res.data.content || []); 
    } catch (error) {
      console.error("Lỗi tải bài viết:", error);
    } finally {
      setLoadingPosts(false);
    }
  };

  const handleRemovePost = (deletedPostId: number) => {
    setPosts(prev => prev.filter(p => p.id !== deletedPostId));
  };

  // --- 2. USE EFFECT (CHẠY 1 LẦN KHI MOUNT) ---
  useEffect(() => {
    // Fetch thông tin User & Bạn bè
    const fetchData = async () => {
      try {
        const [resUser, resFriends] = await Promise.all([
          axiosClient.get('/profile'),
          axiosClient.get('/friends/list')
        ]);

        setUser(resUser.data);
        setFriends(resFriends.data);

        // Đổ dữ liệu vào form edit sẵn
        setEditFormData({
          fullName: resUser.data.fullName,
          className: resUser.data.className,
          bio: resUser.data.bio,
          avatarUrl: resUser.data.avatarUrl
        });

      } catch (error) {
        console.error("Lỗi tải profile:", error);
      }
    };

    fetchData();     // 1. Lấy thông tin user
    fetchMyPosts();  // 2. Lấy danh sách bài viết
  }, []);

  // --- 3. CÁC HÀM XỬ LÝ SỰ KIỆN EDIT ---
  const handleEditChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setEditFormData({ ...editFormData, [e.target.name]: e.target.value });
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setSelectedFile(file);
      setPreviewAvatar(URL.createObjectURL(file));
    }
  };

  const uploadAvatarFile = async (file: File): Promise<string> => {
    const formData = new FormData();
    formData.append("file", file);
    const token = localStorage.getItem("token");
    
    // Gọi API upload (dùng axios gốc để set Content-Type multipart)
    const res = await axios.post("https://mini-social-network-ayab.onrender.com/api/upload/avatar", formData, {
      headers: { 
        "Content-Type": "multipart/form-data",
        "Authorization": `Bearer ${token}` 
      }
    });
    return res.data.url;
  };

  const handleSaveProfile = async () => {
    try {
      let finalAvatarUrl = editFormData.avatarUrl;
      if (selectedFile) {
        finalAvatarUrl = await uploadAvatarFile(selectedFile);
      }

      const updatedData = { ...editFormData, avatarUrl: finalAvatarUrl };
      const res = await axiosClient.put('/profile', updatedData);
      
      setUser(res.data);
      setIsEditing(false);
      alert("Cập nhật thành công!");
    } catch (error) {
      console.error(error);
      alert("Lỗi cập nhật!");
    }
  };

  if (!user) return <div style={{textAlign:'center', marginTop: 100}}>Đang tải...</div>;

  return (
    <>
      {/* <Header /> */}
      <div className="profile-page">
        {/* --- HEADER PROFILE (COVER + AVATAR) --- */}
        <div className="cover-section">
          <div className="cover-photo"></div>
          <div className="profile-header-content">
            <img 
              className="profile-avatar-xl" 
              src={user.avatarUrl || "https://ui-avatars.com/api/?background=random"} 
              alt="ava" 
            />
            <div className="profile-details">
              <h1 className="profile-fullname">{user.fullName}</h1>
              <div className="profile-bio">{user.bio || "Người dùng MiniSocial"}</div>
            </div>
            
            <button className="btn-edit-profile" onClick={() => setIsEditing(true)}>
              ✏️ Chỉnh sửa trang cá nhân
            </button>
          </div>
        </div>

        {/* --- BODY (2 CỘT) --- */}
        <div className="profile-body">
          <div className="profile-container">
            
            {/* CỘT TRÁI (40%): GIỚI THIỆU & BẠN BÈ */}
            <div style={{flex: '4', display: 'flex', flexDirection: 'column', gap: '20px'}}>
              
              {/* Box Giới thiệu */}
              <div className="profile-card">
                <div className="card-title">Giới thiệu</div>
                <div className="info-row">🎓 Sinh viên lớp <b>{user.className}</b></div>
                <div className="info-row">🆔 Mã SV: <b>{user.studentCode}</b></div>
                <div className="info-row">📧 Email: {user.email}</div>
                <div className="info-row">📅 Tham gia: {new Date(user.createdAt).toLocaleDateString()}</div>
              </div>

              {/* Box Bạn bè */}
              <div className="profile-card">
                <div className="card-title" style={{display:'flex', justifyContent:'space-between'}}>
                    <span>Bạn bè</span>
                    <span style={{color:'var(--text-sub)', fontSize:'16px', fontWeight:'normal'}}>{friends.length} người bạn</span>
                </div>
                
                {friends.length > 0 ? (
                    <div style={{display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '10px'}}>
                      {friends.slice(0, 9).map(f => (
                        <div key={f.id} style={{textAlign: 'center'}}>
                            <img 
                              src={f.avatarUrl || `https://ui-avatars.com/api/?name=${f.fullName}`} 
                              style={{width:'100%', aspectRatio:'1/1', borderRadius:'8px', objectFit:'cover', marginBottom:'5px'}} 
                              alt="f"
                            />
                            <div style={{fontSize:'12px', fontWeight:'600', overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap'}}>
                                {f.fullName}
                            </div>
                        </div>
                      ))}
                    </div>
                ) : (
                    <p style={{color:'#888', fontStyle:'italic'}}>Chưa có bạn bè nào.</p>
                )}
                
                <button style={{width:'100%', padding:'8px', marginTop:'15px', background:'#e4e6eb', border:'none', borderRadius:'6px', fontWeight:'600', cursor:'pointer'}}>
                    Xem tất cả bạn bè
                </button>
              </div>
            </div>

            {/* CỘT PHẢI (60%): DANH SÁCH BÀI VIẾT */}
            <div style={{flex: '6'}}>
              <div className="profile-card" style={{padding: '15px', marginBottom: '20px', fontWeight:'bold', fontSize:'18px'}}>
                  Bài viết
              </div>

              {loadingPosts ? (
                  <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
                      <CircularProgress />
                  </Box>
              ) : posts.length > 0 ? (
                  posts.map(post => (
                      <PostCard 
                          key={post.id} 
                          post={post} 
                          onDeleteSuccess={handleRemovePost} 
                      />
                  ))
              ) : (
                  <div className="profile-card" style={{textAlign: 'center', padding: '40px', color: '#65676b'}}>
                      <Typography variant="body1">Bạn chưa đăng bài viết nào.</Typography>
                  </div>
              )}
            </div>

          </div>
        </div>
      </div>

      {/* --- MODAL EDIT PROFILE --- */}
      {isEditing && (
        <div className="modal-overlay">
          <div className="modal-content">
            <span className="close-btn" onClick={() => setIsEditing(false)}>✖</span>
            <div className="modal-header">Chỉnh sửa trang cá nhân</div>
            
            <div className="avatar-upload-box">
              <img 
                  src={previewAvatar || editFormData.avatarUrl || `https://ui-avatars.com/api/?name=${user.fullName}`} 
                  className="edit-avatar-preview"
                  alt="preview"
              />
              <label htmlFor="file-upload" className="camera-icon">📷</label>
              <input id="file-upload" type="file" accept="image/*" onChange={handleFileChange} style={{display:'none'}} />
            </div>

            <div className="modal-form-group">
              <label className="modal-label">Họ và tên</label>
              <input name="fullName" className="modern-input" value={editFormData.fullName || ''} onChange={handleEditChange} />
            </div>
            <div className="modal-form-group">
              <label className="modal-label">Lớp</label>
              <input name="className" className="modern-input" value={editFormData.className || ''} onChange={handleEditChange} />
            </div>
            <div className="modal-form-group">
              <label className="modal-label">Tiểu sử (Bio)</label>
              <textarea name="bio" className="modern-input" style={{height: '80px', resize: 'none', fontFamily:'inherit'}} value={editFormData.bio || ''} onChange={handleEditChange} />
            </div>

            <div className="btn-actions">
              <button className="btn-cancel" onClick={() => setIsEditing(false)}>Hủy</button>
              <button className="btn-save" onClick={handleSaveProfile}>Lưu thay đổi</button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default Profile;