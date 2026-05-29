import axiosClient from './axiosClient';

// Hàm lấy lịch sử tin nhắn an toàn
export const getMessagesHistory = async (currentUserId: number, targetUserId: number) => {
  // 1. Mã hóa an toàn ID (Ngăn chặn Path Traversal / Tainted Data)
  const safeCurrentId = encodeURIComponent(currentUserId);
  const safeTargetId = encodeURIComponent(targetUserId);

  return axiosClient.get(`/messages/${safeCurrentId}/${safeTargetId}`);
};

// Hàm lấy danh sách cuộc trò chuyện gần đây (Không cần tham số, nhưng đưa vào API layer cho chuẩn)
export const getRecentConversations = async () => {
    return axiosClient.get('/messages/recent');
  };

  // Hàm đánh dấu tin nhắn đã đọc (Có tham số cần mã hóa)
  export const markMessageAsRead = async (partnerId: number) => {
    // 1. Mã hóa an toàn ID (Vá lỗ hổng Tainted Data / Path Traversal)
    const safePartnerId = encodeURIComponent(partnerId);
    return axiosClient.put(`/messages/read/${safePartnerId}`);
  };