// ==========================================
// THÔNG TIN NGƯỜI DÙNG (USER)
// ==========================================
export interface User {
  id: number;
  studentCode: string;
  email: string;
  fullName: string;
  className: string;
  role: string;
  
  avatarUrl?: string; // Có dấu ? vì có thể user chưa có ảnh
  coverPhotoUrl?: string; // 🟢 ĐÃ THÊM: Ảnh bìa (Cover Photo)
  
  bio?: string;
  active: boolean;
  
  // 🟢 Điểm và Gamification
  vptlPoints?: number; 
  currentAvatarFrame?: string | null; 
  currentNameColor?: string | null; 
  
  createdAt: string;
  lastLogin: string;
}

export interface UserSummary {
  id: number;
  fullName: string;
  avatarUrl?: string;
  coverPhotoUrl?: string; // 🟢 ĐÃ THÊM: Đề phòng UI cần hiển thị
  studentCode: string;
}

export interface UpdateProfileData {
  fullName?: string;
  className?: string;
  bio?: string;
  avatarUrl?: string;
  coverPhotoUrl?: string; // 🟢 ĐÃ THÊM: Dữ liệu ảnh bìa khi update
}

export interface AuthResponse {
  token: string;
}

// ==========================================
// BÀI VIẾT & BÌNH LUẬN (POST & COMMENT)
// ==========================================
export interface CommentData {
  id: number;
  content: string;
  author: UserSummary;
  createdAt: string;
  likeCount: number;
  replyCount: number;
  parentId: number | null;
  likedByCurrentUser: boolean;
  replies?: CommentData[]; // Dùng cho UI logic
}

// ==========================================
// NHẮN TIN (CHAT)
// ==========================================
export interface Conversation {
  partnerId: number;
  partnerName: string;
  partnerAvatar?: string;
  lastMessage: string;
  timestamp: string;
  isRead: boolean;
}