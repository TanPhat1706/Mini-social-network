// src/types/auth.ts

export interface IUser {
    id: number;
    studentCode: string;
    email: string;
    fullName: string;
    role: string;
    // Thêm các trường khác nếu cần (fullName, role...)
}

export interface IJwtResponse {
    accessToken: string;
    refreshToken: string;
    type: string;
    id: number;
    studentCode: string;
    email: string;
    fullName: string;
    role: string;
}

export interface IAuthContext {
    currentUser: IJwtResponse | null;
    login: (studentCode: string, password: string) => Promise<IJwtResponse>;
    logout: () => void;
    accessToken: string | null;
}

export interface ITokenRefreshResponse {
    accessToken: string;
    refreshToken: string;
    tokenType: string;
}

export interface IRegisterData {
    studentCode: string;
    email: string;
    password: string;
    fullName: string;
    className: string; 
}