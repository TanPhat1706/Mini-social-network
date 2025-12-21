export interface User {
  id: number;
  studentCode: string;
  email: string;
  fullName: string;
  className: string;
  role: string;
  avatarUrl?: string;
  bio?: string;
  active: boolean;
  createdAt: string;
  lastLogin: string;
}

export interface AuthResponse {
  token: string;
}

export interface UpdateProfileData {
  fullName?: string;
  className?: string;
  bio?: string;
  avatarUrl?: string;
}