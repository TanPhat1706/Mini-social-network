import axios from 'axios';
import { getApiBaseUrl } from '../config/apiBase';

const API_URL = `${getApiBaseUrl()}/api/games`; 

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
    userId: number;
    gameKey: string;
    score: number;
}

// Helper lấy Header chứa Token
const getAuthHeader = () => {
    const token = localStorage.getItem('token'); 
    return {
        headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
            'ngrok-skip-browser-warning': 'true',
        }
    };
};

// 3. Object chứa các hàm gọi API
const gameApi = {
    /**
     * Lấy bảng xếp hạng (Public - Không cần Token)
     */
    getLeaderboard: async (gameKey: string): Promise<GameScore[]> => {
        try {
            // Bây giờ biến API_URL đã tồn tại, dòng này sẽ chạy đúng
            const response = await axios.get<GameScore[]>(`${API_URL}/leaderboard/${gameKey}`, {
                headers: { 'ngrok-skip-browser-warning': 'true' },
            });
            return response.data;
        } catch (error) {
            console.error("Lỗi lấy BXH:", error);
            return []; 
        }
    },

    /**
     * Lưu điểm số (Private - Cần Token)
     */
    saveScore: async (data: SaveScoreRequest): Promise<GameScore | null> => {
        try {
            console.log("Đang gọi API lưu điểm tới:", `${API_URL}/score`); // Log để kiểm tra
            const response = await axios.post(`${API_URL}/score`, data, getAuthHeader());
            return response.data.data; 
        } catch (error) {
            console.error("Lỗi lưu điểm:", error);
            throw error; 
        }
    }
};

export default gameApi;