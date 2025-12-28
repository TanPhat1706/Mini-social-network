export interface User {
    id: number;
    username: string;
    fullName: string;
    avatarUrl: string;
}

export interface Media {
    id: number;
    url: string;
    type: "IMAGE" | "VIDEO";
}

export interface Post {
    id: number;
    content: string;
    visibility: "PUBLIC" | "CLASS" | "PRIVATE" | "PENDING";
    createdAt: string;
    updatedAt: string;
    author: User;
    media: Media[];
    likeCount: number;
    commentCount: number;
    likedByCurrentUser: boolean;
}

