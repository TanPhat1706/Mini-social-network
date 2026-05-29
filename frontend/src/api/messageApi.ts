import axiosClient from './axiosClient';

// Tối ưu hóa: Hàm rào chắn kiểm tra ID hợp lệ (Ngăn chặn triệt để Tainted Data từ localStorage)
const validateId = (id: any): number => {
  const validId = Number(id);
  if (!Number.isInteger(validId) || validId <= 0) {
    throw new Error("Invalid ID detected");
  }
  return validId;
};

// Hàm lấy lịch sử tin nhắn an toàn
export const getMessagesHistory = async (currentUserId: number, targetUserId: number) => {
  // 1. Ép kiểu và kiểm tra số nguyên an toàn
  const safeCurrentId = validateId(currentUserId);
  const safeTargetId = validateId(targetUserId);

  return axiosClient.get(`/messages/${safeCurrentId}/${safeTargetId}`);
};

// Hàm lấy danh sách cuộc trò chuyện gần đây (Không cần tham số, nhưng đưa vào API layer cho chuẩn)
export const getRecentConversations = async () => {
  return axiosClient.get('/messages/recent');
};

// Hàm đánh dấu tin nhắn đã đọc (Có tham số cần validate)
export const markMessageAsRead = async (partnerId: number) => {
  // 1. Ép kiểu và kiểm tra số nguyên an toàn
  const safePartnerId = validateId(partnerId);
  return axiosClient.put(`/messages/read/${safePartnerId}`);
};