export interface User {
    id: number;
    fullName: string;
    studentCode: string;
    email: string;
    className: string;
    bio: string;
    avatarUrl: string;
    role: string;
    active: boolean;
    vptl_Points: number;
    currentAvatarFrame: string | null; // MỚI: Thêm trường này để lưu frame hiện tại của user
    createdAt: string;
    lastLogin: string;
}

export interface UserSummary {
    id: number;
    fullName: string;
    avatarUrl: string;
    studentCode: string;
    currentAvatarFrame?: string | null;
    currentNameColor?: string | null;
}

export interface UpdateProfileData {
  fullName?: string;
  className?: string;
  bio?: string;
  avatarUrl?: string;
}

export interface CommentData {
    id: number;
    content: string;
    author: UserSummary;
    createdAt: string;
    likeCount: number;
    replyCount: number;
    parentId: number | null;
    likedByCurrentUser: boolean;
    // Dùng cho UI logic
    replies?: CommentData[]; 
}

export interface Conversation {
    partnerId: number;
    partnerName: string;
    partnerAvatar: string;
    lastMessage: string;
    timestamp: string;
    isRead: boolean;
}