import axiosClient from './axiosClient';

export interface FriendshipStatusResponse {
  status: string;
  actionUserId?: number | null;
}

export const getFriendshipStatus = async (targetUserId: number) => {
  return axiosClient.get<FriendshipStatusResponse>(`/friends/status/${targetUserId}`);
};

export const sendFriendRequest = async (targetUserId: number) => {
  return axiosClient.post(`/friends/add/${targetUserId}`);
};

export const acceptFriendRequest = async (targetUserId: number) => {
  return axiosClient.post(`/friends/accept/${targetUserId}`);
};

export const removeFriendship = async (targetUserId: number) => {
  return axiosClient.delete(`/friends/remove/${targetUserId}`);
};
