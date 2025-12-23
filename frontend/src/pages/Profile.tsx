import React, { useEffect, useState } from 'react';
import Header from '../components/Header';
import axiosClient from '../api/axiosClient';
import axios from 'axios'; // Dùng axios gốc để upload file
import type { User, UpdateProfileData } from '../types';
import './Profile.css';
import FriendButton from '../components/FriendButton';

const Profile: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const [friends, setFriends] = useState<User[]>([]);
  
  // --- KHÔI PHỤC LẠI STATE EDIT ---
  const [isEditing, setIsEditing] = useState(false);
  const [editFormData, setEditFormData] = useState<UpdateProfileData>({});
  const [previewAvatar, setPreviewAvatar] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  useEffect(() => {
    Promise.all([
      axiosClient.get('/profile'),
      axiosClient.get('/friends/list')
    ]).then(([resUser, resFriends]) => {
      setUser(resUser.data);
      // Đổ dữ liệu vào form edit
      setEditFormData({
        fullName: resUser.data.fullName,
        className: resUser.data.className,
        bio: resUser.data.bio,
        avatarUrl: resUser.data.avatarUrl
      });
      setFriends(resFriends.data);
    });
  }, []);

  // --- CÁC HÀM XỬ LÝ (ĐÃ KHÔI PHỤC) ---
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
    const token = localStorage.getItem("token"); // Lấy token thủ công tránh lỗi 403
    
    // Gọi API upload riêng (http://localhost:8080...)
    const res = await axios.post("http://localhost:8080/api/upload/avatar", formData, {
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
      // Gọi API update profile (chú ý đường dẫn khớp với axios của bạn)
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
      <Header />
      <div className="profile-page">
        <div className="cover-section">
          <div className="cover-photo"></div>
          <div className="profile-header-content">
            <img className="profile-avatar-xl" src={user.avatarUrl || "https://ui-avatars.com/api/?background=random"} alt="ava" />
            <div className="profile-details">
              <h1 className="profile-fullname">{user.fullName}</h1>
              <div className="profile-bio">{user.bio || "Người dùng MiniSocial"}</div>
            </div>
            
            {/* --- NÚT BẤM ĐÃ ĐƯỢC GẮN LẠI SỰ KIỆN --- */}
            <button className="btn-edit-profile" onClick={() => setIsEditing(true)}>
              ✏️ Chỉnh sửa trang cá nhân
            </button>

          </div>
        </div>

        <div className="profile-body">
          <div className="profile-container">
            {/* Cột trái: Giới thiệu */}
            <div style={{flex: '4'}}>
              <div className="profile-card">
                <div className="card-title">Giới thiệu</div>
                <div className="info-row">🎓 Sinh viên lớp <b>{user.className}</b></div>
                <div className="info-row">🆔 Mã SV: <b>{user.studentCode}</b></div>
                <div className="info-row">📧 Email: {user.email}</div>
                <div className="info-row">📅 Tham gia: {new Date(user.createdAt).toLocaleDateString()}</div>
              </div>
            </div>

            {/* Cột phải: Bạn bè */}
            <div style={{flex: '6'}}>
              <div className="profile-card">
                 <div className="card-title" style={{display:'flex', justifyContent:'space-between'}}>
                    <span>Bạn bè</span>
                    <span style={{color:'var(--text-sub)', fontSize:'16px', fontWeight:'normal'}}>{friends.length} người bạn</span>
                 </div>
                 
                 {friends.length > 0 ? (
                    <div style={{display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '15px'}}> {/* Chỉnh lại repeat(2, 1fr) cho rộng rãi hơn để chứa nút */}
                      {friends.map(f => (
                        <div key={f.id} style={{textAlign: 'left', border: '1px solid #f0f2f5', padding: '10px', borderRadius: '8px'}}>
                           <div style={{display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '10px'}}>
                              <img src={f.avatarUrl} style={{width:'50px', height:'50px', borderRadius:'50%', objectFit:'cover'}} alt="f"/>
                              <div style={{fontWeight:'600', fontSize:'14px', overflow:'hidden', textOverflow:'ellipsis'}}>{f.fullName}</div>
                           </div>
                           
                           {/* NHÚNG NÚT HỦY KẾT BẠN VÀO ĐÂY */}
                           <FriendButton targetUserId={f.id} currentUserId={user.id} />
                        </div>
                      ))}
                    </div>
                 ) : (
                    <p style={{color:'#888', fontStyle:'italic'}}>Chưa có bạn bè nào.</p>
                 )}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* --- PHẦN MODAL (POPUP) ĐÃ ĐƯỢC KHÔI PHỤC --- */}
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
              <input 
                name="fullName" 
                className="modern-input" 
                value={editFormData.fullName || ''} 
                onChange={handleEditChange} 
              />
            </div>

            <div className="modal-form-group">
              <label className="modal-label">Lớp</label>
              <input 
                name="className" 
                className="modern-input" 
                value={editFormData.className || ''} 
                onChange={handleEditChange} 
              />
            </div>

            <div className="modal-form-group">
              <label className="modal-label">Tiểu sử (Bio)</label>
              <textarea 
                name="bio" 
                className="modern-input" 
                style={{height: '80px', resize: 'none', fontFamily:'inherit'}}
                value={editFormData.bio || ''} 
                onChange={handleEditChange} 
              />
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