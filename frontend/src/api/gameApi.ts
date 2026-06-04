import api from './api';

// 2. Định nghĩa kiểu dữ liệu (Interfaces)
export interface GameScore {
    id: number;
    gameKey: string;
    userId: number;
    username: string;
    avatarUrl: string | null;
    score: number;
    playedAt: string;
}

export interface SaveScoreRequest {
    gameKey: string;
    score: number;
}

// 3. Object chứa các hàm gọi API
const gameApi = {
    /**
     * Lấy bảng xếp hạng (Public - Không cần Token)
     * Sử dụng api instance để đảm bảo baseURL nhất quán trên mọi môi trường (local/cloud)
     */
    getLeaderboard: async (gameKey: string): Promise<GameScore[]> => {
        try {
            const response = await api.get<GameScore[]>(`/api/games/leaderboard/${gameKey}`);
            return response.data;
        } catch (error) {
            console.error("Lỗi lấy BXH:", error);
            return [];
        }
    },

    /**
     * Lưu điểm số (Private - Token được gắn tự động qua interceptor)
     */
    saveScore: async (data: SaveScoreRequest): Promise<GameScore | null> => {
        try {
            console.log("Đang gọi API lưu điểm:", data);
            // Chỉ gửi gameKey + score — backend tự lấy userId từ JWT
            const response = await api.post('/api/games/score', {
                gameKey: data.gameKey,
                score: data.score,
            });
            return response.data.data;
        } catch (error) {
            console.error("Lỗi lưu điểm:", error);
            throw error;
        }
    }
};

export default gameApi;