import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useParams, useNavigate } from 'react-router-dom';
import axiosClient from '../api/axiosClient'; // Dùng cho User/Profile
import api from '../api/api'; // Dùng cho Post/Friend (để thống nhất logic token)
import { CircularProgress, Box, Typography } from '@mui/material';

import type { User } from '../types';
import type { UpdateProfileData } from '../types/types';
import type { PostData } from '../components/post/CardPost';

import PostCard from '../components/post/CardPost';
import './Profile.css';

// Định nghĩa trạng thái trả về từ backend
// Lưu ý: Backend bạn trả về object, ví dụ { "status": "FRIEND" }
type FriendStatus = 'FRIEND' | 'PENDING_SENT' | 'PENDING_RECEIVED' | 'NONE' | 'SELF';

const Profile: React.FC = () => {
  const { id } = useParams<{ id: string }>(); 
  const navigate = useNavigate();

  // ===== STATE =====
  const [profileUser, setProfileUser] = useState<User | null>(null); // Người sở hữu profile
  const [currentUser, setCurrentUser] = useState<User | null>(null); // Người đang đăng nhập
  
  const [friends, setFriends] = useState<User[]>([]);
  const [posts, setPosts] = useState<PostData[]>([]);
  const [loadingPosts, setLoadingPosts] = useState(false);
  const [friendStatus, setFriendStatus] = useState<FriendStatus>('NONE');

  // Edit State
  const [isEditing, setIsEditing] = useState(false);
  const [editFormData, setEditFormData] = useState<UpdateProfileData>({});
  const [previewAvatar, setPreviewAvatar] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  // Logic xác định trang của mình:
  // 1. Không có ID trên URL (ví dụ /profile)
  // 2. HOẶC ID trên URL trùng với ID của người đang login
  const isOwnProfile = !id || (currentUser && String(currentUser.id) === id);

  // ===== API CALLS =====

  useEffect(() => {
    const fetchProfileData = async () => {
      try {
        // 1. Lấy thông tin người đang đăng nhập (để biết mình là ai)
        const resMe = await axiosClient.get('/profile');
        const myData = resMe.data;
        setCurrentUser(myData);

        // Xác định ID của profile đang xem
        let targetUserId = id ? id : myData.id;
        let userData;

        // --- FETCH USER INFO ---
        if (!id || String(myData.id) === id) {
          // Xem trang chính mình
          userData = myData;
          setFriendStatus('SELF');
          
          // Fill dữ liệu vào form edit
          setEditFormData({
            fullName: myData.fullName,
            className: myData.className,
            bio: myData.bio,
            avatarUrl: myData.avatarUrl,
          });
        } else {
          // Xem trang người khác -> Gọi API lấy info user
          // Giả sử endpoint lấy user theo ID là /users/{id} (Check lại UserControlller của bạn)
          const resUser = await axiosClient.get(`/users/${targetUserId}`); 
          userData = resUser.data;
          
          // Check trạng thái bạn bè
          try {
            // SỬA: Endpoint khớp với FriendController: /api/auth/friends/status/{id}
            const resStatus = await api.get(`/api/auth/friends/status/${targetUserId}`);
            // Backend trả về: { "status": "FRIEND", ... } hoặc chuỗi trực tiếp tùy code
            // Giả sử trả về object { status: "..." } hoặc string
            const statusRaw = resStatus.data.status || resStatus.data; 
            setFriendStatus(statusRaw);
          } catch (err) {
            console.warn("Lỗi check status friend", err);
            setFriendStatus('NONE'); 
          }
        }
        setProfileUser(userData);

        // --- FETCH FRIEND LIST ---
        // SỬA: Endpoint khớp với FriendController: /api/auth/friends/list/{id}
        // Nếu là mình thì gọi /list, người khác gọi /list/{id}
        let friendListUrl = '/api/auth/friends/list';
        if (id && String(myData.id) !== id) {
             friendListUrl = `/api/auth/friends/list/${targetUserId}`;
        }
        const resFriends = await api.get(friendListUrl);
        setFriends(resFriends.data);

        // --- FETCH POSTS ---
        // Gọi hàm tách riêng để xử lý logic endpoint
        fetchPosts(targetUserId, !id || String(myData.id) === id);

      } catch (e) {
        console.error("Lỗi tải trang cá nhân:", e);
      }
    };

    fetchProfileData();
  }, [id]); 

  // Hàm lấy bài viết (SỬA ĐỂ KHỚP VỚI PostService BẠN GỬI)
  const fetchPosts = async (targetUserId: string | number, isMyProfile: boolean) => {
    try {
      setLoadingPosts(true);
      let url = '';
      
      if (isMyProfile) {
        // Post của chính mình: /api/posts/my-posts
        url = '/api/posts/my-posts';
      } else {
        // Post người khác: /api/posts/my-posts/{authorId}
        url = `/api/posts/my-posts/${targetUserId}`;
      }

      const res = await api.get(url);
      // Backend trả về Page<PostResponse>, dữ liệu thực nằm trong content
      setPosts(res.data.content || []);
    } catch (e) {
      console.error("Lỗi load bài viết:", e);
    } finally {
      setLoadingPosts(false);
    }
  };

  // ===== HANDLERS FRIEND ACTION =====
  const handleFriendAction = async () => {
    if (!profileUser) return;
    try {
      const targetId = profileUser.id;

      // SỬA: Các Endpoint khớp hoàn toàn với FriendController
      if (friendStatus === 'NONE') {
        // /api/auth/friends/add/{targetId}
        await api.post(`/api/auth/friends/add/${targetId}`);
        setFriendStatus('PENDING_SENT');
      } 
      else if (friendStatus === 'PENDING_RECEIVED') {
        // /api/auth/friends/accept/{targetId}
        await api.post(`/api/auth/friends/accept/${targetId}`);
        setFriendStatus('FRIEND');
      } 
      else if (friendStatus === 'FRIEND' || friendStatus === 'PENDING_SENT') {
        if (window.confirm("Bạn có chắc chắn muốn thực hiện thao tác này?")) {
            // /api/auth/friends/remove/{targetId}
            await api.delete(`/api/auth/friends/remove/${targetId}`);
            setFriendStatus('NONE');
        }
      }
    } catch (error) {
      console.error(error);
      alert("Thao tác thất bại.");
    }
  };

  const handleRemovePost = (postId: number) => {
    setPosts(prev => prev.filter(p => p.id !== postId));
  };

  // ... (Phần logic Edit Profile giữ nguyên) ...
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
    // Lưu ý: Update lại URL upload nếu bạn đã chuyển sang Cloudinary hoặc endpoint khác
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
      setProfileUser(res.data);
      setCurrentUser(res.data);
      setIsEditing(false);
      alert('Cập nhật thành công!');
    } catch (e) {
      alert('Lỗi cập nhật!');
    }
  };

  // Render Button
  const renderActionButton = () => {
    if (isOwnProfile) {
      return (
        <button className="edit-btn" onClick={() => setIsEditing(true)}>
          ✏️ Chỉnh sửa
        </button>
      );
    }

    switch (friendStatus) {
      case 'FRIEND':
        return <button className="friend-btn friend" onClick={handleFriendAction}>✅ Bạn bè</button>;
      case 'PENDING_SENT':
        return <button className="friend-btn pending" onClick={handleFriendAction}>📩 Đã gửi lời mời</button>;
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
                    onClick={() => navigate(`/profile/${f.id}`)}
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

      {/* MODAL EDIT */}
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