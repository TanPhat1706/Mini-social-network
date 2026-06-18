import axiosClient from './axiosClient';

export interface FriendshipStatusResponse {
  status: string;
  actionUserId?: number | null;
}

export const getFriendshipStatus = async (targetUserId: number) => {
  // 1. Mã hóa an toàn ID đầu vào (Ngăn chặn Path Traversal / Injection)
  const safeTargetUserId = encodeURIComponent(targetUserId);
  return axiosClient.get<FriendshipStatusResponse>(`/friends/status/${safeTargetUserId}`);
};

export const sendFriendRequest = async (targetUserId: number) => {
  const safeTargetUserId = encodeURIComponent(targetUserId);
  return axiosClient.post(`/friends/add/${safeTargetUserId}`);
};

export const acceptFriendRequest = async (targetUserId: number) => {
  const safeTargetUserId = encodeURIComponent(targetUserId);
  return axiosClient.post(`/friends/accept/${safeTargetUserId}`);
};

export const removeFriendship = async (targetUserId: number) => {
  const safeTargetUserId = encodeURIComponent(targetUserId);
  return axiosClient.delete(`/friends/remove/${safeTargetUserId}`);
};