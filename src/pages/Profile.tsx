import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useParams, useNavigate } from 'react-router-dom'; // Import router hooks
import axiosClient from '../api/axiosClient';
import api from '../api/api';
import { CircularProgress, Box, Typography } from '@mui/material';

import type { User } from '../types';
import type { UpdateProfileData } from '../types/types';
import type { PostData } from '../components/post/CardPost';

import PostCard from '../components/post/CardPost';
import './Profile.css';

// Định nghĩa các trạng thái bạn bè
type FriendStatus = 'FRIEND' | 'PENDING_SENT' | 'PENDING_RECEIVED' | 'NONE' | 'SELF';

const Profile: React.FC = () => {
  const { id } = useParams<{ id: string }>(); // Lấy ID từ URL
  const navigate = useNavigate();

  // ===== STATE =====
  const [profileUser, setProfileUser] = useState<User | null>(null); // User của profile đang xem
  const [currentUser, setCurrentUser] = useState<User | null>(null); // User đang đăng nhập
  
  const [friends, setFriends] = useState<User[]>([]);
  const [posts, setPosts] = useState<PostData[]>([]);
  const [loadingPosts, setLoadingPosts] = useState(false);
  const [friendStatus, setFriendStatus] = useState<FriendStatus>('NONE');

  // Edit State
  const [isEditing, setIsEditing] = useState(false);
  const [editFormData, setEditFormData] = useState<UpdateProfileData>({});
  const [previewAvatar, setPreviewAvatar] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  // Xác định xem đây có phải trang của mình không
  // Là mình nếu: Không có ID trên URL HOẶC ID trên URL trùng với ID của current user
  const isOwnProfile = !id || (currentUser && String(currentUser.id) === id);

  // ===== API CALLS =====

  // 1. Fetch dữ liệu Profile
  useEffect(() => {
    const fetchProfileData = async () => {
      try {
        // Luôn lấy thông tin người đang đăng nhập trước để so sánh
        const resMe = await axiosClient.get('/profile');
        const myData = resMe.data;
        setCurrentUser(myData);

        let targetUserId = id ? id : myData.id;
        let userData;

        // Nếu là trang của mình
        if (!id || String(myData.id) === id) {
          userData = myData;
          setFriendStatus('SELF');
          // Setup form edit sẵn
          setEditFormData({
            fullName: myData.fullName,
            className: myData.className,
            bio: myData.bio,
            avatarUrl: myData.avatarUrl,
          });
        } 
        // Nếu là trang người khác
        else {
          const resUser = await axiosClient.get(`/users/${targetUserId}`); // Cần API lấy info user theo ID
          userData = resUser.data;
          
          // Kiểm tra trạng thái bạn bè
          try {
            // TODO: Bạn cần backend API trả về: { status: 'FRIEND' | 'PENDING_SENT' ... }
            const resStatus = await axiosClient.get(`/friends/status/${targetUserId}`);
            setFriendStatus(resStatus.data.status);
          } catch (err) {
            console.warn("Chưa có API check status, mặc định là NONE");
            setFriendStatus('NONE'); 
          }
        }

        setProfileUser(userData);

        // Lấy danh sách bạn bè của user này
        const resFriends = await axiosClient.get(`/friends/list/${targetUserId}`); // Cần API list friend theo ID
        setFriends(resFriends.data);

        // Lấy danh sách bài viết
        fetchUserPosts(targetUserId);

      } catch (e) {
        console.error("Lỗi tải trang cá nhân:", e);
        // Có thể navigate về trang 404 nếu lỗi
      }
    };

    fetchProfileData();
  }, [id]); // Chạy lại mỗi khi ID trên URL thay đổi

  const fetchUserPosts = async (userId: string | number) => {
    try {
      setLoadingPosts(true);
      // Gọi API lấy bài viết của user cụ thể
      const res = await api.get(`/api/posts/user/${userId}`); 
      setPosts(res.data.content || []);
    } catch (e) {
      console.error(e);
    } finally {
      setLoadingPosts(false);
    }
  };

  // ===== HANDLERS =====

  const handleFriendAction = async () => {
    if (!profileUser) return;
    try {
      const targetId = profileUser.id;

      if (friendStatus === 'NONE') {
        // Gửi lời mời
        await axiosClient.post(`/friends/request/${targetId}`);
        setFriendStatus('PENDING_SENT');
      } 
      else if (friendStatus === 'PENDING_RECEIVED') {
        // Chấp nhận
        await axiosClient.put(`/friends/accept/${targetId}`);
        setFriendStatus('FRIEND');
      } 
      else if (friendStatus === 'FRIEND' || friendStatus === 'PENDING_SENT') {
        // Hủy kết bạn / Hủy lời mời
        if (window.confirm("Bạn có chắc chắn muốn thực hiện thao tác này?")) {
            await axiosClient.delete(`/friends/remove/${targetId}`);
            setFriendStatus('NONE');
        }
      }
    } catch (error) {
      alert("Thao tác thất bại. Kiểm tra lại API Backend.");
    }
  };

  const handleRemovePost = (postId: number) => {
    setPosts(prev => prev.filter(p => p.id !== postId));
  };

  // ... (Giữ nguyên logic Edit Profile cũ của bạn) ...
  const handleEditChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
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
      { headers: { 'Content-Type': 'multipart/form-data', Authorization: `Bearer ${token}` } }
    );
    return res.data.url;
  };

  const handleSaveProfile = async () => {
    try {
      let avatarUrl = editFormData.avatarUrl;
      if (selectedFile) avatarUrl = await uploadAvatarFile(selectedFile);
      const res = await axiosClient.put('/profile', { ...editFormData, avatarUrl });
      setProfileUser(res.data); // Update UI
      setCurrentUser(res.data); // Update Context user
      setIsEditing(false);
      alert('Cập nhật thành công!');
    } catch (e) {
      alert('Lỗi cập nhật!');
    }
  };

  // Render nút bấm tùy theo ngữ cảnh
  const renderActionButton = () => {
    if (isOwnProfile) {
      return (
        <button className="edit-btn" onClick={() => setIsEditing(true)}>
          ✏️ Chỉnh sửa
        </button>
      );
    }

    // Logic hiển thị nút kết bạn
    switch (friendStatus) {
      case 'FRIEND':
        return <button className="friend-btn friend" onClick={handleFriendAction}>✅ Bạn bè</button>;
      case 'PENDING_SENT':
        return <button className="friend-btn pending" onClick={handleFriendAction}>📩 Đã gửi</button>;
      case 'PENDING_RECEIVED':
        return <button className="friend-btn accept" onClick={handleFriendAction}>🤝 Chấp nhận</button>;
      case 'NONE':
      default:
        return <button className="friend-btn add" onClick={handleFriendAction}>➕ Thêm bạn bè</button>;
    }
  };

  if (!profileUser) return <div className="loading">Đang tải...</div>;

  return (
    <>
      <div className="profile-page">
        {/* COVER */}
        <div className="cover-section">
          <div className="cover-photo" />
          <div className="profile-header">
            <img
              className="avatar-large"
              src={profileUser.avatarUrl || `https://ui-avatars.com/api/?name=${profileUser.fullName}`}
              alt="avatar"
            />
            <div className="profile-info">
              <h1>{profileUser.fullName}</h1>
              <p>{profileUser.bio || 'Người dùng MiniSocial'}</p>
            </div>
            {/* NÚT ACTION (Sửa hoặc Kết bạn) */}
            {renderActionButton()}
          </div>
        </div>

        {/* BODY */}
        <div className="profile-body">
          <div className="left-col">
            <div className="card">
              <h3>Giới thiệu</h3>
              <p>🎓 Lớp: <b>{profileUser.className}</b></p>
              <p>🆔 MSSV: <b>{profileUser.studentCode}</b></p>
              <p>📧 {profileUser.email}</p>
            </div>

            <div className="card">
              <h3>Bạn bè ({friends.length})</h3>
              <div className="friends-grid">
                {friends.slice(0, 6).map(f => (
                  <div 
                    key={f.id} 
                    className="friend-item"
                    onClick={() => navigate(`/profile/${f.id}`)} // Chuyển trang khi click vào bạn
                  >
                    <img src={f.avatarUrl || `https://ui-avatars.com/api/?name=${f.fullName}`} alt={f.fullName} />
                    <span>{f.fullName}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div className="right-col">
            <div className="card title">Bài viết</div>

            {loadingPosts ? (
              <Box sx={{ display: 'flex', justifyContent: 'center' }}><CircularProgress /></Box>
            ) : posts.length ? (
              posts.map(p => (
                <PostCard 
                  key={p.id} 
                  post={p} 
                  onDeleteSuccess={handleRemovePost}
                  // Chỉ cho phép xóa nếu là bài của mình (hoặc admin)
                  canDelete={isOwnProfile} 
                />
              ))
            ) : (
              <div className="card empty">
                <Typography>Chưa có bài viết nào</Typography>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* MODAL EDIT (Chỉ hiện khi là trang của mình) */}
      {isOwnProfile && isEditing && (
        <div className="modal-overlay">
          <div className="modal">
            <div className="modal-header">
              <h2>Chỉnh sửa trang cá nhân</h2>
              <span onClick={() => setIsEditing(false)}>✖</span>
            </div>
            <div className="avatar-edit">
              <img src={previewAvatar || editFormData.avatarUrl || ''} alt="preview" />
              <label htmlFor="file">📷</label>
              <input id="file" type="file" hidden onChange={handleFileChange} />
            </div>
            <div className="form">
              <input name="fullName" placeholder="Họ và tên" value={editFormData.fullName || ''} onChange={handleEditChange} />
              <input name="className" placeholder="Lớp" value={editFormData.className || ''} onChange={handleEditChange} />
              <textarea name="bio" placeholder="Tiểu sử" value={editFormData.bio || ''} onChange={handleEditChange} />
            </div>
            <div className="actions">
              <button onClick={() => setIsEditing(false)}>Hủy</button>
              <button className="primary" onClick={handleSaveProfile}>Lưu</button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default Profile;