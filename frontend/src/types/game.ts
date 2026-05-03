// Định nghĩa các hằng số (Value)
export const GameEventValues = {
    GAME_INVITE_ACCEPTED: 'GAME_INVITE_ACCEPTED',
    ROOM_JOINED: 'ROOM_JOINED',
    GAME_START: 'GAME_START',
    GAME_UPDATE: 'GAME_UPDATE',
    GAME_END: 'GAME_END',
    ERROR: 'ERROR'
} as const;

// Tạo Type từ các giá trị trên (Dùng để định nghĩa kiểu dữ liệu)
export type GameEventType = (typeof GameEventValues)[keyof typeof GameEventValues];

export interface GameSession {
    id: number;
    board: string[][];
    status: string;
    hostId: number;
    guestId: number;
    winnerId?: number;
    currentTurnId: number;
}

export interface GameWsEvent {
    type: GameEventType; // Bây giờ TS đã hiểu đây là Type
    message: string;
    session: GameSession;
    actorId?: number;
    row?: number;
    col?: number;
}