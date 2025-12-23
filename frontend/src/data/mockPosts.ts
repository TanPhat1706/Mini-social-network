import type { Post } from '../types';

export const mockPosts: Post[] = [
  {
    id: 1,
    author: {
      id: 101,
      fullName: "Nguyễn Văn A",
      avatarUrl: "https://ui-avatars.com/api/?name=Nguyen+Van+A&background=random"
    },
    content: "Hôm nay trời đẹp quá, đi học code thôi mọi người ơi! 🌞 #coding #studentlife",
    imageUrl: "https://picsum.photos/seed/post1/600/300", // Ảnh ngẫu nhiên
    createdAt: "2023-10-27T08:30:00Z",
    likesCount: 120,
    commentsCount: 45
  },
  {
    id: 2,
    author: {
      id: 102,
      fullName: "Trần Thị B",
      avatarUrl: "https://ui-avatars.com/api/?name=Tran+Thi+B&background=random"
    },
    content: "Vừa hoàn thành xong project cuối khóa, nhẹ cả người. Chuẩn bị ăn mừng thôi! 🎉🎉🎉",
    createdAt: "2023-10-26T15:45:00Z",
    likesCount: 89,
    commentsCount: 12
  },
  {
    id: 3,
    author: {
      id: 103,
      fullName: "Lê Hoàng C",
      avatarUrl: "https://ui-avatars.com/api/?name=Le+Hoang+C&background=random"
    },
    content: "Có ai biết chỗ nào bán cà phê ngon gần trường không nhỉ? Đang cần nạp caffeine gấp.",
    createdAt: "2023-10-26T10:00:00Z",
    likesCount: 34,
    commentsCount: 89
  },
  {
    id: 4,
    author: {
      id: 101,
      fullName: "Nguyễn Văn A",
      avatarUrl: "https://ui-avatars.com/api/?name=Nguyen+Van+A&background=random"
    },
    content: "Check-in thư viện trường. View xịn xò thực sự.",
    imageUrl: "https://picsum.photos/seed/library/600/400",
    createdAt: "2023-10-25T09:15:00Z",
    likesCount: 256,
    commentsCount: 31
  },
  {
    id: 5,
    author: {
      id: 104,
      fullName: "Phạm Min D",
      avatarUrl: "https://ui-avatars.com/api/?name=Pham+Min+D&background=random"
    },
    content: "Mọi người nhớ deadline nộp bài tập lớn tuần sau nhé. Đừng để nước đến chân mới nhảy!",
    createdAt: "2023-10-24T20:30:00Z",
    likesCount: 55,
    commentsCount: 5
  },
];