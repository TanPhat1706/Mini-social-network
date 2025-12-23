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
  createdAt: string;
  lastLogin: string;
}

export interface Post {
  id: number;
  author: {
    id: number;
    fullName: string;
    avatarUrl: string;
  };
  content: string;
  imageUrl?: string;
  createdAt: string;
  likesCount: number;
  commentsCount: number;
}

export interface UpdateProfileData {
  fullName?: string;
  className?: string;
  bio?: string;
  avatarUrl?: string;
}