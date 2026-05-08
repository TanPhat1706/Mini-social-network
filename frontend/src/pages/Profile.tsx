import React, { useEffect, useState } from 'react';
import axiosClient from '../api/axiosClient';
import { CircularProgress, Box, Typography, Button } from '@mui/material';

// Import Types & Components
import type { User, UpdateProfileData } from '../types';
import PostCard from '../components/post/CardPost';
import type { PostData } from '../components/post/CardPost';
import './Profile.css';
import api from '../api/api';

// 🔴 IMPORT MA THUẬT CSS VÀO ĐÂY
import AvatarWithFrame from '../components/AvatarWithFrame';
import ColoredName from '../components/ColoredName'; // (Sửa đường dẫn cho đúng từng file)

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
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [saveMessage, setSaveMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const BIO_MAX_LENGTH = 160;

  // --- 1. CÁC HÀM API ---
  const fetchMyPosts = async () => {
    try {
      setLoadingPosts(true);
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
    const fetchData = async () => {
      try {
        const [resUser, resFriends] = await Promise.all([
          axiosClient.get('/profile'),
          axiosClient.get('/friends/list')
        ]);

        setUser(resUser.data);
        setFriends(resFriends.data);

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

    fetchData();
    fetchMyPosts();
  }, []);

  // --- 3. CÁC HÀM XỬ LÝ SỰ KIỆN EDIT ---
  const handleEditChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    if (name === 'bio') {
      setEditFormData({ ...editFormData, bio: value.slice(0, BIO_MAX_LENGTH) });
      return;
    }
    setEditFormData({ ...editFormData, [name]: value });
  };

  const resetEditForm = () => {
    if (!user) return;
    setEditFormData({
      fullName: user.fullName,
      className: user.className,
      bio: user.bio || '',
      avatarUrl: user.avatarUrl
    });
    setPreviewAvatar(null);
    setSelectedFile(null);
    setSaveMessage(null);
  };

  const handleOpenEditModal = () => {
    resetEditForm();
    setIsEditing(true);
  };

  const handleCloseEditModal = () => {
    setIsEditing(false);
    setSaveMessage(null);
  };

  useEffect(() => {
    if (!isEditing) return;

    const handleEsc = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        handleCloseEditModal();
      }
    };

    globalThis.addEventListener('keydown', handleEsc);
    return () => globalThis.removeEventListener('keydown', handleEsc);
  }, [isEditing]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setSelectedFile(file);
    setPreviewAvatar(URL.createObjectURL(file));
  };

  const handleSaveProfile = async () => {
    try {
      setIsSavingProfile(true);
      setSaveMessage(null);

      const fullName = (editFormData.fullName || '').trim();
      const className = (editFormData.className || '').trim();
      const bio = (editFormData.bio || '').trim().slice(0, BIO_MAX_LENGTH);

      if (!fullName) {
        setSaveMessage({ type: 'error', text: 'Họ và tên không được để trống.' });
        return;
      }

      const formData = new FormData();
      formData.append('fullName', fullName);
      formData.append('className', className);
      formData.append('bio', bio);
      if (selectedFile) {
        formData.append('avatar', selectedFile);
      }

      const res = await axiosClient.put('/profile', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });

      setUser(res.data);
      alert("Cập nhật thành công!");
      setIsEditing(false);
    } catch (error) {
      console.error(error);
      setSaveMessage({ type: 'error', text: 'Lỗi cập nhật, vui lòng thử lại.' });
    } finally {
      setIsSavingProfile(false);
    }
  };

  // 🔴 MỚI: HÀM GỠ VIỀN AVATAR
  const handleUnequipFrame = async () => {
    try {
      const res = await api.put('/api/shop/items/unequip');
      // Cập nhật lại state ảo để UI mất viền ngay lập tức
      user && setUser({ ...user, currentAvatarFrame: undefined } as any);
      alert(res.data.message || "Đã tháo viền Avatar thành công!");
    } catch (error) {
      console.error("Lỗi tháo viền:", error);
      alert("Không thể tháo viền lúc này.");
    }
  };

  if (!user) return <div style={{ textAlign: 'center', marginTop: 100 }}>Đang tải...</div>;

  const bioLength = (editFormData.bio || '').length;
  const disableSave = isSavingProfile || !(editFormData.fullName || '').trim();
  let avatarPreviewSrc = `https://ui-avatars.com/api/?name=${user.fullName}`;
  if (editFormData.avatarUrl) avatarPreviewSrc = editFormData.avatarUrl;
  if (previewAvatar) avatarPreviewSrc = previewAvatar;
  let postsSection: React.ReactNode;
  if (loadingPosts) {
    postsSection = (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <CircularProgress />
      </Box>
    );
  } else if (posts.length > 0) {
    postsSection = (
      posts.map(post => (
        <PostCard
          key={post.id}
          post={post}
          onDeleteSuccess={handleRemovePost}
        />
      ))
    );
  } else {
    postsSection = (
      <div className="profile-card" style={{ textAlign: 'center', padding: '40px', color: '#65676b' }}>
        <Typography variant="body1">Bạn chưa đăng bài viết nào.</Typography>
      </div>
    );
  }

  return (
    <>
      <div className="profile-page">
        {/* --- HEADER PROFILE (COVER + AVATAR) --- */}
        <div className="cover-section">
          <div className="cover-photo"></div>
          <div className="profile-header-content">

            {/* 🔴 THAY THẾ ẢNH AVATAR CHÍNH BẰNG COMPONENT CÓ VIỀN */}
            <div className="profile-avatar-xl" style={{ border: 'none', background: 'transparent', padding: 0 }}>
              <AvatarWithFrame
                src={user.avatarUrl || `https://ui-avatars.com/api/?name=${user.fullName}`}
                frameClass={(user as any).currentAvatarFrame}
                size={140}
              />
            </div>

            <div className="profile-details" style={{ marginTop: '10px' }}>
              <h1 className="profile-fullname">
                <ColoredName name={user.fullName} colorClass={(user as any).currentNameColor} />
              </h1>              <div className="profile-bio">{user.bio || "Người dùng MiniSocial"}</div>
            </div>

            <button className="btn-edit-profile" onClick={handleOpenEditModal}>
              ✏️ Chỉnh sửa trang cá nhân
            </button>
          </div>
        </div>

        {/* --- BODY (2 CỘT) --- */}
        <div className="profile-body">
          <div className="profile-container">

            {/* CỘT TRÁI (40%): GIỚI THIỆU & BẠN BÈ */}
            <div style={{ flex: '4', display: 'flex', flexDirection: 'column', gap: '20px' }}>

              <div className="profile-card">
                <div className="card-title">Giới thiệu</div>
                <div className="info-row">🎓 Sinh viên lớp <b>{user.className}</b></div>
                <div className="info-row">🆔 Mã SV: <b>{user.studentCode}</b></div>
                <div className="info-row">📧 Email: {user.email}</div>
                <div className="info-row">📅 Tham gia: {new Date(user.createdAt).toLocaleDateString()}</div>
              </div>

              {/* Box Bạn bè */}
              <div className="profile-card">
                <div className="card-title" style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span>Bạn bè</span>
                  <span style={{ color: 'var(--text-sub)', fontSize: '16px', fontWeight: 'normal' }}>{friends.length} người bạn</span>
                </div>

                {friends.length > 0 ? (
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '15px' }}>
                    {friends.slice(0, 9).map(f => (
                      <div key={f.id} style={{ textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                        {/* 🔴 ÁP DỤNG VIỀN CHO AVATAR BẠN BÈ */}
                        <AvatarWithFrame
                          src={f.avatarUrl || `https://ui-avatars.com/api/?name=${f.fullName}`}
                          frameClass={(f as any).currentAvatarFrame}
                          size={80}
                        />
                        <div style={{ fontSize: '12px', fontWeight: '600', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', marginTop: '5px', width: '100%' }}>
                          <ColoredName name={f.fullName} colorClass={(f as any).currentNameColor} />
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p style={{ color: '#888', fontStyle: 'italic' }}>Chưa có bạn bè nào.</p>
                )}

                <button style={{ width: '100%', padding: '8px', marginTop: '15px', background: '#e4e6eb', border: 'none', borderRadius: '6px', fontWeight: '600', cursor: 'pointer' }}>
                  Xem tất cả bạn bè
                </button>
              </div>
            </div>

            {/* CỘT PHẢI (60%): DANH SÁCH BÀI VIẾT */}
            <div style={{ flex: '6' }}>
              <div className="profile-card" style={{ padding: '15px', marginBottom: '20px', fontWeight: 'bold', fontSize: '18px' }}>
                Bài viết
              </div>

              {postsSection}
            </div>

          </div>
        </div>
      </div>

      {/* --- MODAL EDIT PROFILE --- */}
      {isEditing && (
        <div className="modal-overlay">
          <div
            className="modal-content profile-edit-modal"
            aria-labelledby="edit-profile-title"
          >
            <button type="button" className="close-btn" onClick={handleCloseEditModal} aria-label="Đóng popup">✖</button>
            <div className="modal-header" id="edit-profile-title">Chỉnh sửa trang cá nhân</div>
            <div className="modal-subtitle">Cập nhật thông tin để hồ sơ của bạn nổi bật và chuyên nghiệp hơn.</div>

            {saveMessage && (
              <div className={`modal-alert modal-alert-${saveMessage.type}`}>
                {saveMessage.text}
              </div>
            )}

            {/* 🔴 ÁP DỤNG VIỀN TRONG MODAL & THÊM NÚT GỠ VIỀN */}
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginBottom: '20px' }}>
              <div className="avatar-upload-box" style={{ position: 'relative', width: '100px', height: '100px', margin: '0 auto' }}>
                <AvatarWithFrame
                  src={avatarPreviewSrc}
                  frameClass={(user as any).currentAvatarFrame}
                  size={100}
                />
                <label htmlFor="file-upload" className="camera-icon" style={{ zIndex: 10 }}>📷</label>
                <input id="file-upload" type="file" accept="image/*" onChange={handleFileChange} style={{ display: 'none' }} />
              </div>

              {/* NÚT THÁO VIỀN */}
              {(user as any).currentAvatarFrame && (
                <Button
                  variant="outlined"
                  color="error"
                  size="small"
                  sx={{ mt: 2, borderRadius: '50px', textTransform: 'none', fontWeight: 'bold' }}
                  onClick={handleUnequipFrame}
                >
                  🚫 Tháo viền Avatar
                </Button>
              )}
            </div>

            <div className="modal-form-group">
              <label className="modal-label" htmlFor="edit-fullName">Họ và tên</label>
              <input id="edit-fullName" name="fullName" className="modern-input" value={editFormData.fullName || ''} onChange={handleEditChange} />
            </div>
            <div className="modal-form-group">
              <label className="modal-label" htmlFor="edit-className">Lớp</label>
              <input id="edit-className" name="className" className="modern-input" value={editFormData.className || ''} onChange={handleEditChange} />
            </div>
            <div className="modal-form-group">
              <label className="modal-label" htmlFor="edit-bio">Tiểu sử (Bio)</label>
              <textarea
                id="edit-bio"
                name="bio"
                className="modern-input profile-bio-textarea"
                value={editFormData.bio || ''}
                onChange={handleEditChange}
                placeholder="Viết vài dòng giới thiệu về bạn..."
              />
              <div className={`bio-counter ${bioLength > BIO_MAX_LENGTH - 20 ? 'near-limit' : ''}`}>
                {bioLength}/{BIO_MAX_LENGTH}
              </div>
            </div>

            <div className="btn-actions">
              <button className="btn-cancel" onClick={handleCloseEditModal} disabled={isSavingProfile}>Hủy</button>
              <button className="btn-save" onClick={handleSaveProfile} disabled={disableSave}>
                {isSavingProfile ? 'Đang lưu...' : 'Lưu thay đổi'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default Profile;