import React, { useEffect, useState } from 'react';
import axiosClient from '../api/axiosClient';
import axios from 'axios'; // Dùng axios gốc để upload file
import { useAuth } from '../context/AuthContext';
import type { User, UpdateProfileData } from '../types';

const Profile: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [loading, setLoading] = useState(true);

  // State cho form chỉnh sửa
  const [editFormData, setEditFormData] = useState<UpdateProfileData>({});
  
  // State xử lý ảnh: Preview (xem trước) và File (để gửi đi)
  const [previewAvatar, setPreviewAvatar] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const { logout } = useAuth();

  // 1. Tải dữ liệu User khi vào trang
  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const res = await axiosClient.get('/profile');
        setUser(res.data);
        // Đổ dữ liệu hiện tại vào form edit để sẵn sàng sửa
        setEditFormData({
          fullName: res.data.fullName,
          className: res.data.className,
          bio: res.data.bio,
          avatarUrl: res.data.avatarUrl
        });
        setLoading(false);
      } catch (error) {
        logout(); // Token lỗi thì logout
      }
    };
    fetchProfile();
  }, [logout]);

  // 2. Xử lý khi nhập liệu text
  const handleEditChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setEditFormData({ ...editFormData, [e.target.name]: e.target.value });
  };

  // 3. Xử lý khi chọn ảnh từ máy (Preview ngay lập tức)
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setSelectedFile(file);
      setPreviewAvatar(URL.createObjectURL(file)); // Tạo link giả để xem trước
    }
  };

  // 4. Hàm Upload ảnh lên Server (ĐÃ SỬA LỖI 403)
  const uploadAvatarFile = async (file: File): Promise<string> => {
    const formData = new FormData();
    formData.append("file", file);
    
    // Lấy token thủ công
    const token = localStorage.getItem("token");

    // Gửi request với Header Authorization
    const res = await axios.post("http://localhost:8080/api/upload/avatar", formData, {
      headers: { 
        "Content-Type": "multipart/form-data",
        "Authorization": `Bearer ${token}` 
      }
    });
    return res.data.url;
  };

  // 5. Lưu toàn bộ thay đổi
  const handleSaveProfile = async () => {
    try {
      let finalAvatarUrl = editFormData.avatarUrl;

      // Nếu có chọn file ảnh mới, upload nó trước
      if (selectedFile) {
        finalAvatarUrl = await uploadAvatarFile(selectedFile);
      }

      // Gửi thông tin text kèm URL ảnh mới lên server
      const updatedData = { ...editFormData, avatarUrl: finalAvatarUrl };
      const res = await axiosClient.put('/profile', updatedData);
      
      setUser(res.data); // Cập nhật UI chính
      setIsEditing(false); // Đóng modal
      setSelectedFile(null); // Reset file
      alert("Cập nhật hồ sơ thành công!");
    } catch (error) {
      console.error(error);
      alert("Có lỗi xảy ra khi cập nhật!");
    }
  };

  if (loading) return <div style={{textAlign: 'center', marginTop: '50px', color: '#666'}}>Đang tải dữ liệu...</div>;
  if (!user) return null;

  return (
    <div className="profile-wrapper">
      <div className="profile-card">
        {/* === HEADER ẢNH BÌA === */}
        <div className="profile-cover">
           {/* Nút Logout nhỏ gọn ở góc */}
           <button onClick={logout} className="btn-logout" style={{position: 'absolute', top: 20, right: 20, fontSize: '12px', padding: '5px 15px'}}>
              Đăng xuất
           </button>
        </div>

        {/* === AVATAR & INFO === */}
        <div className="profile-avatar-container">
          <img 
            className="profile-avatar"
            src={user.avatarUrl || `https://ui-avatars.com/api/?name=${user.fullName}&background=random&size=150`} 
            alt="Avatar" 
          />
        </div>

        <div className="profile-info">
          <h2 className="profile-name">
            {user.fullName} 
            {user.active && <span style={{color: '#42b72a', fontSize: '14px', marginLeft: '5px', verticalAlign: 'middle'}} title="Đang hoạt động">●</span>}
          </h2>
          <span className="profile-role">{user.role}</span>
          
          <p style={{ marginTop: '15px', color: '#555', fontStyle: 'italic', maxWidth: '600px', margin: '15px auto' }}>
            {user.bio ? `"${user.bio}"` : "Người dùng này chưa viết tiểu sử."}
          </p>

          {/* Stats Bar (Giả lập cho giống Social Network) */}
          <div className="profile-stats">
            <div className="stat-item">
              <h4>12</h4>
              <p>Bài viết</p>
            </div>
            <div className="stat-item">
              <h4>{user.studentCode}</h4>
              <p>Mã SV</p>
            </div>
            <div className="stat-item">
              <h4>540</h4>
              <p>Bạn bè</p>
            </div>
          </div>

          <button 
            onClick={() => setIsEditing(true)} 
            className="btn-primary"
            style={{maxWidth: '200px', margin: '0 auto', background: 'white', color: 'var(--primary-color)', border: '2px solid var(--primary-color)'}}
          >
            ✏️ Chỉnh sửa trang cá nhân
          </button>

          {/* === GRID THÔNG TIN CHI TIẾT === */}
          <div className="profile-details-grid">
            <div className="detail-box">
              <div className="detail-label">📧 Email liên hệ</div>
              <div className="detail-value">{user.email}</div>
            </div>
            <div className="detail-box">
              <div className="detail-label">🎓 Lớp học</div>
              <div className="detail-value">{user.className || "Chưa cập nhật"}</div>
            </div>
            <div className="detail-box">
              <div className="detail-label">📅 Ngày tham gia</div>
              <div className="detail-value">{new Date(user.createdAt).toLocaleDateString('vi-VN')}</div>
            </div>
            <div className="detail-box">
              <div className="detail-label">⏰ Đăng nhập cuối</div>
              <div className="detail-value">{new Date(user.lastLogin).toLocaleString('vi-VN')}</div>
            </div>
          </div>
        </div>
      </div>

      {/* === MODAL CHỈNH SỬA (POPUP) === */}
      {isEditing && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <span>Chỉnh sửa thông tin</span>
              <span onClick={() => setIsEditing(false)} style={{cursor: 'pointer', float: 'right'}}>✖</span>
            </div>
            
            {/* 1. UPLOAD AVATAR SECTION */}
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginBottom: '20px' }}>
              <div className="avatar-upload-wrapper">
                <img 
                  src={previewAvatar || editFormData.avatarUrl || `https://ui-avatars.com/api/?name=${user.fullName}&background=random`} 
                  alt="Preview" 
                  style={{ width: '100px', height: '100px', borderRadius: '50%', objectFit: 'cover', border: '4px solid #f0f2f5' }}
                />
                {/* Nút Camera đè lên ảnh */}
                <label htmlFor="file-upload" className="camera-overlay">
                  📷
                </label>
                <input id="file-upload" type="file" accept="image/*" onChange={handleFileChange} />
              </div>
              <span style={{fontSize: '12px', color: '#888', marginTop: '5px'}}>Nhấn vào máy ảnh để đổi avatar</span>
            </div>

            {/* 2. FORM INPUTS */}
            <div className="form-group">
              <label style={{fontSize: '13px', fontWeight: '600', color: '#444'}}>Họ và tên</label>
              <input name="fullName" className="form-input" value={editFormData.fullName || ''} onChange={handleEditChange} />
            </div>

            <div className="form-group">
              <label style={{fontSize: '13px', fontWeight: '600', color: '#444'}}>Lớp</label>
              <input name="className" className="form-input" value={editFormData.className || ''} onChange={handleEditChange} />
            </div>

            <div className="form-group">
              <label style={{fontSize: '13px', fontWeight: '600', color: '#444'}}>Tiểu sử (Bio)</label>
              <textarea 
                name="bio" 
                className="form-input" 
                style={{height: '80px', resize: 'none', fontFamily: 'inherit'}} 
                value={editFormData.bio || ''} 
                onChange={handleEditChange} 
                placeholder="Giới thiệu ngắn về bản thân..."
              />
            </div>

            <div className="modal-actions">
              <button className="btn-secondary" onClick={() => setIsEditing(false)}>Hủy bỏ</button>
              <button className="btn-save" onClick={handleSaveProfile}>Lưu thay đổi</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Profile;