import React, { useState, useEffect, useRef } from 'react';
import gameApi, { type GameScore } from '../../api/gameApi';
import { ArrowBack } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';

// --- ⚙️ CẤU HÌNH HỆ THỐNG ---
const CANVAS_SIZE = 400;
const SCALE = 20;
const START_SNAKE: number[][] = [[8, 8], [8, 9]];
const START_DIR: number[] = [0, -1];

// 🔴 ĐỔI THÀNH TRUE KHI DEPLOY LÊN SERVER ĐỂ BẬT GIỚI HẠN 🔴
const ENABLE_DAILY_LIMIT = true; 
const MAX_PLAYS_PER_DAY = 2;

const SnakeGame: React.FC = () => {
    const navigate = useNavigate();
    
    // --- STATE ---
    const [snake, setSnake] = useState<number[][]>(START_SNAKE);
    const [food, setFood] = useState<number[]>([5, 5]);
    const [dir, setDir] = useState<number[]>(START_DIR);
    const [score, setScore] = useState<number>(0);
    const [gameOver, setGameOver] = useState<boolean>(false);
    const [isPlaying, setIsPlaying] = useState<boolean>(false);
    const [leaderboard, setLeaderboard] = useState<GameScore[]>([]);

    const gameBoardRef = useRef<HTMLDivElement>(null);
    const isSavedRef = useRef(false);
    const lastDirRef = useRef<number[]>(START_DIR); 

    // --- 🧬 TỪ ĐIỂN TIẾN HÓA (8 TIER) ---
    const getTier = (currentScore: number) => {
        if (currentScore < 20) return 0;   // Tier 0: Đồ đá (Mờ, giật)
        if (currentScore < 50) return 1;   // Tier 1: Quay số Dial-up (Đỡ mờ)
        if (currentScore < 90) return 2;   // Tier 2: Tiêu chuẩn CRT (Bình thường)
        if (currentScore < 140) return 3;  // Tier 3: Màn hình LCD (Sắc nét)
        if (currentScore < 200) return 4;  // Tier 4: Gaming Gear (Xanh Neon)
        if (currentScore < 280) return 5;  // Tier 5: Cyberpunk (Tím Neon)
        if (currentScore < 380) return 6;  // Tier 6: Thần Thoại (Lục bảo rực sáng)
        return 7;                          // Tier 7: Hoàng Kim (Vàng lấp lánh)
    };

    const currentTier = getTier(score);

    // Đường cong độ khó mượt mà hơn cho 8 cấp
    const getDynamicSpeed = (tier: number) => {
        const speeds = [280, 230, 180, 140, 110, 85, 65, 50];
        return speeds[tier] || 150;
    };

    const currentSpeed = getDynamicSpeed(currentTier);

    // --- API LOGIC ---
    useEffect(() => {
        fetchLeaderboard();
    }, []);

    const fetchLeaderboard = async () => {
        try {
            const data = await gameApi.getLeaderboard("SNAKE");
            setLeaderboard(data);
        } catch (error) {
            console.error("Lỗi tải BXH", error);
        }
    };

    // --- GAME LOOP ---
    useEffect(() => {
        if (!isPlaying) return;
        const moveSnake = setInterval(() => {
            setSnake((prevSnake) => {
                lastDirRef.current = dir; 

                const maxCells = CANVAS_SIZE / SCALE;
                let newX = prevSnake[0][0] + dir[0];
                let newY = prevSnake[0][1] + dir[1];

                // Logic xuyên hầm
                if (newX >= maxCells) newX = 0;
                else if (newX < 0) newX = maxCells - 1;

                if (newY >= maxCells) newY = 0;
                else if (newY < 0) newY = maxCells - 1;

                const newHead = [newX, newY];

                if (checkCollision(newHead, prevSnake)) {
                    endGame();
                    return prevSnake;
                }
                
                const newSnake = [newHead, ...prevSnake];
                
                if (newHead[0] === food[0] && newHead[1] === food[1]) {
                    setScore((s) => s + 10);
                    generateFood();
                } else {
                    newSnake.pop();
                }
                return newSnake;
            });
        }, currentSpeed);
        
        return () => clearInterval(moveSnake);
    }, [isPlaying, dir, food, currentSpeed]);

    const checkCollision = (head: number[], snakeBody: number[][]) => {
        for (const segment of snakeBody) {
            if (head[0] === segment[0] && head[1] === segment[1]) return true;
        }
        return false;
    };

    const handleKeyDown = (e: React.KeyboardEvent<HTMLDivElement>) => {
        const keyMap: { [key: string]: number[] } = {
            ArrowUp: [0, -1], ArrowDown: [0, 1], ArrowLeft: [-1, 0], ArrowRight: [1, 0]
        };
        if (keyMap[e.key]) {
            e.preventDefault();
            const lastDir = lastDirRef.current; 
            const newDir = keyMap[e.key];
            
            if (newDir[0] !== 0 && newDir[0] === -lastDir[0]) return;
            if (newDir[1] !== 0 && newDir[1] === -lastDir[1]) return;
            
            setDir(newDir);
        }
    };

    const generateFood = () => {
        const x = Math.floor(Math.random() * (CANVAS_SIZE / SCALE));
        const y = Math.floor(Math.random() * (CANVAS_SIZE / SCALE));
        setFood([x, y]);
    };

    const checkDailyLimit = (): boolean => {
        if (!ENABLE_DAILY_LIMIT) return true; // Nếu chưa bật flag thì cho chơi thoải mái (không giới hạn)

        const userStr = localStorage.getItem('user');
        const userId = userStr ? JSON.parse(userStr).id : 'guest';
        
        // Lấy ngày hiện tại theo chuẩn YYYY-MM-DD
        const today = new Date().toISOString().split('T')[0]; 
        const storageKey = `snake_play_limit_${userId}`;
        const limitDataStr = localStorage.getItem(storageKey);
        
        let limitData = limitDataStr ? JSON.parse(limitDataStr) : { date: today, count: 0 };

        // Sang ngày mới thì reset
        if (limitData.date !== today) {
            limitData = { date: today, count: 0 };
        }

        if (limitData.count >= MAX_PLAYS_PER_DAY) {
            alert(`⏳ Bạn đã hết lượt chơi hôm nay (${MAX_PLAYS_PER_DAY}/${MAX_PLAYS_PER_DAY}). Hãy quay lại vào ngày mai để tiếp tục cày top nhé!`);
            return false;
        }

        // Tăng lượt chơi và lưu lại
        limitData.count += 1;
        localStorage.setItem(storageKey, JSON.stringify(limitData));
        return true;
    };

    const startGame = () => {
        if (!checkDailyLimit()) return; // Kiểm tra giới hạn trước khi bắt đầu

        setSnake(START_SNAKE);
        setScore(0);
        setDir(START_DIR);
        lastDirRef.current = START_DIR;
        setGameOver(false);
        setIsPlaying(true);
        generateFood();
        isSavedRef.current = false;
        setTimeout(() => gameBoardRef.current?.focus(), 100);
    };

    const endGame = () => {
        setIsPlaying(false);
        setGameOver(true);
    };

    useEffect(() => {
        if (gameOver) {
            saveScoreToBackend();
        }
    }, [gameOver]);

    const saveScoreToBackend = async () => {
        if (isSavedRef.current) return;
        if (score === 0) return;

        try {
            isSavedRef.current = true; 
            const userStr = localStorage.getItem('user');
            if (!userStr) return;

            const user = JSON.parse(userStr);
            await gameApi.saveScore({ userId: user.id, gameKey: "SNAKE", score: score });
            fetchLeaderboard();
        } catch (error) {
            console.error("Lỗi lưu điểm", error);
        }
    };

    // --- 🎨 TIẾN HÓA ĐỒ HỌA (8 TIERS) ---
    const getBoardStyle = () => {
        let baseStyle: React.CSSProperties = {
            position: 'relative', width: CANVAS_SIZE, height: CANVAS_SIZE,
            backgroundColor: '#111827', border: '4px solid #374151', 
            borderRadius: '8px', outline: 'none', cursor: 'pointer', overflow: 'hidden',
            transition: 'all 1s ease-in-out'
        };

        if (currentTier === 0) {
            baseStyle.filter = 'grayscale(100%) blur(2px)';
            baseStyle.backgroundColor = '#000';
            baseStyle.border = '4px solid #222';
            baseStyle.borderRadius = '0px';
        } else if (currentTier === 1) {
            baseStyle.filter = 'sepia(50%) blur(0.5px)';
            baseStyle.borderRadius = '2px';
        } else if (currentTier >= 4) { // Hiệu ứng lưới Grid từ Tier 4 trở đi
            baseStyle.backgroundImage = `linear-gradient(rgba(255,255,255,0.03) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.03) 1px, transparent 1px)`;
            baseStyle.backgroundSize = `${SCALE}px ${SCALE}px`;
        }

        // Màu viền và glow theo Tier cao
        if (currentTier === 4) { baseStyle.boxShadow = '0 0 15px rgba(34, 197, 94, 0.2)'; baseStyle.borderColor = '#22c55e'; }
        if (currentTier === 5) { baseStyle.boxShadow = '0 0 20px rgba(168, 85, 247, 0.4)'; baseStyle.borderColor = '#a855f7'; }
        if (currentTier === 6) { baseStyle.boxShadow = '0 0 25px rgba(6, 182, 212, 0.5)'; baseStyle.borderColor = '#06b6d4'; }
        if (currentTier === 7) { 
            baseStyle.boxShadow = '0 0 35px rgba(250, 204, 21, 0.6)'; 
            baseStyle.borderColor = '#eab308'; // Vàng hoàng kim
            baseStyle.backgroundColor = '#1c1917'; // Nền hơi ngả nâu đen sang trọng
        }
        
        return baseStyle;
    };

    const getSnakeSegmentStyle = (x: number, y: number, isHead: boolean) => {
        let style: React.CSSProperties = {
            position: 'absolute', width: SCALE, height: SCALE,
            left: x * SCALE, top: y * SCALE, zIndex: 10,
            transition: 'all 0.05s linear'
        };

        // Từ Tier 0 đến Tier 3: Trắng đen mờ -> Xanh lá cơ bản
        if (currentTier === 0) { style.backgroundColor = '#9ca3af'; style.border = 'none'; } 
        else if (currentTier === 1) { style.backgroundColor = isHead ? '#65a30d' : '#84cc16'; style.border = '1px solid #3f6212'; } 
        else if (currentTier === 2) { style.backgroundColor = isHead ? '#16a34a' : '#4ade80'; style.border = '1px solid #14532d'; style.borderRadius = isHead ? '4px' : '2px'; } 
        else if (currentTier === 3) { style.backgroundColor = isHead ? '#15803d' : '#22c55e'; style.borderRadius = isHead ? '8px' : '4px'; } 
        
        // Từ Tier 4 trở đi: Neon, Phát sáng, Hình thù xịn xò
        else if (currentTier === 4) { style.backgroundColor = isHead ? '#22c55e' : '#4ade80'; style.borderRadius = isHead ? '50%' : '6px'; style.boxShadow = '0 0 8px #4ade80'; } 
        else if (currentTier === 5) { style.backgroundColor = isHead ? '#a855f7' : '#c084fc'; style.borderRadius = isHead ? '50%' : '8px'; style.boxShadow = '0 0 10px #c084fc'; style.border = '1px solid #f3e8ff'; } 
        else if (currentTier === 6) { style.backgroundColor = isHead ? '#06b6d4' : '#22d3ee'; style.borderRadius = isHead ? '50%' : '10px'; style.boxShadow = '0 0 15px #22d3ee'; style.border = '2px solid #ecfeff'; } 
        
        // Cảnh giới cao nhất: HOÀNG KIM (Vàng kim nhấp nháy)
        else if (currentTier === 7) { 
            style.background = isHead ? 'linear-gradient(135deg, #fbbf24, #f59e0b)' : 'linear-gradient(135deg, #fcd34d, #fbbf24)';
            style.borderRadius = isHead ? '50%' : '4px';
            style.border = '1px solid #fef08a';
            // Box-shadow này kết hợp với class .golden-sparkle ở hàm render
        }
        return style;
    };

    const getFoodStyle = (x: number, y: number) => {
        let style: React.CSSProperties = {
            position: 'absolute', width: SCALE, height: SCALE,
            left: x * SCALE, top: y * SCALE, zIndex: 5,
            backgroundColor: currentTier === 0 ? '#9ca3af' : '#ef4444',
            borderRadius: currentTier >= 2 ? '50%' : '0%',
        };
        
        if (currentTier >= 4) style.boxShadow = '0 0 10px #f87171';
        
        // Đồ ăn Tier cuối: Viên Kim Cương Vàng
        if (currentTier === 7) {
            style.background = 'radial-gradient(circle, #fff, #fbbf24, #d97706)';
            style.borderRadius = '2px';
            style.transform = 'rotate(45deg)'; // Xoay thành hình thoi
            style.boxShadow = '0 0 15px #fde047, 0 0 30px #fbbf24';
        }
        return style;
    };

    const getTierName = () => {
        switch(currentTier) {
            case 0: return "Kỷ Nguyên Đồ Đá 🪨";
            case 1: return "Mạng Dial-up 📞";
            case 2: return "Tiêu Chuẩn CRT 📺";
            case 3: return "Màn Hình LCD 💻";
            case 4: return "Gaming Gear Neon 🟢";
            case 5: return "Cyberpunk 2077 🟣";
            case 6: return "Thần Thoại Lục Bảo 🔵";
            case 7: return "Đế Tôn Hoàng Kim 👑";
            default: return "";
        }
    };

    // --- RENDER UI ---
    return (
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '20px', padding: '20px', backgroundColor: '#f9fafb', borderRadius: '10px', boxShadow: '0 4px 6px rgba(0,0,0,0.1)', maxWidth: '600px', margin: '40px auto' }}>
            
            {/* Inject CSS Animations cho Cấp Độ Hoàng Kim */}
            <style>
                {`
                    @keyframes pulse-food {
                        0% { transform: scale(0.9) ${currentTier === 7 ? 'rotate(45deg)' : ''}; opacity: 0.8; }
                        50% { transform: scale(1.15) ${currentTier === 7 ? 'rotate(45deg)' : ''}; opacity: 1; }
                        100% { transform: scale(0.9) ${currentTier === 7 ? 'rotate(45deg)' : ''}; opacity: 0.8; }
                    }
                    .food-animated { animation: pulse-food 0.8s infinite ease-in-out; }
                    
                    /* Hiệu ứng lấp lánh chói lóa cho rắn hoàng kim */
                    @keyframes golden-glow {
                        0% { box-shadow: 0 0 5px #fde047; filter: brightness(1); }
                        50% { box-shadow: 0 0 20px #fbbf24, 0 0 30px #fff; filter: brightness(1.3); }
                        100% { box-shadow: 0 0 5px #fde047; filter: brightness(1); }
                    }
                    .golden-sparkle { animation: golden-glow 1s infinite alternate; }
                `}
            </style>

            <div style={{ width: '100%', maxWidth: '600px', marginBottom: '10px' }}>
                <button 
                    onClick={() => navigate('/games')}
                    style={{ display: 'flex', alignItems: 'center', gap: '5px', background: 'none', border: 'none', cursor: 'pointer', color: '#4b5563', fontWeight: 'bold', fontSize: '16px' }}
                >
                    <ArrowBack /> Quay lại danh sách
                </button>
            </div>
            
            <div style={{ textAlign: 'center' }}>
                <h1 style={{ fontSize: '2rem', fontWeight: 'bold', 
                             color: currentTier === 7 ? '#d97706' : '#16a34a', 
                             textShadow: currentTier === 7 ? '0 0 10px #fde047' : 'none',
                             margin: '0 0 5px 0', transition: 'all 1s' }}>
                    🐍 Rắn Săn Mồi Tiến Hóa
                </h1>
                <p style={{ color: '#6b7280', fontSize: '1.1rem', margin: 0 }}>
                    Điểm số: <span style={{ fontWeight: 'bold', color: '#000', fontSize: '1.5rem' }}>{score}</span>
                </p>
                <p style={{ fontSize: '1rem', fontWeight: 'bold', 
                            color: currentTier === 7 ? '#eab308' : '#ef4444', 
                            marginTop: '5px' }}>
                </p>
            </div>

            {/* GAME BOARD */}
            <div 
                ref={gameBoardRef}
                tabIndex={0}
                onKeyDown={handleKeyDown}
                style={getBoardStyle()}
            >
                {snake.map((seg, i) => (
                    <div 
                        key={i} 
                        className={currentTier === 7 ? "golden-sparkle" : ""}
                        style={getSnakeSegmentStyle(seg[0], seg[1], i === 0)} 
                    />
                ))}
                
                <div 
                    className={currentTier >= 3 ? "food-animated" : ""} 
                    style={getFoodStyle(food[0], food[1])} 
                />

                {!isPlaying && (
                    <div style={{ position: 'absolute', inset: 0, backgroundColor: 'rgba(0, 0, 0, 0.85)', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', color: 'white', zIndex: 20 }}>
                        {gameOver && <h2 style={{ fontSize: '28px', color: '#ef4444', marginBottom: '10px', fontWeight: 'bold' }}>Game Over!</h2>}
                        <p style={{ marginBottom: '15px', color: '#d1d5db', fontSize: '18px' }}>Điểm của bạn: <strong style={{color: '#fff'}}>{score}</strong></p>
                        
                        <button 
                            onClick={startGame} 
                            style={{ 
                                padding: '12px 28px', 
                                backgroundColor: currentTier === 7 ? '#d97706' : '#16a34a', 
                                color: 'white', fontWeight: 'bold', border: 'none', 
                                borderRadius: '50px', fontSize: '16px', cursor: 'pointer', 
                                marginTop: '10px', boxShadow: currentTier === 7 ? '0 0 15px #fde047' : 'none'
                            }}
                        >
                            {gameOver ? "Chơi Lại Ngay" : "Bắt Đầu Game"}
                        </button>
                    </div>
                )}
            </div>

            {/* LEADERBOARD */}
            <div style={{ width: '100%', backgroundColor: 'white', padding: '15px', borderRadius: '8px', boxShadow: '0 1px 3px rgba(0,0,0,0.1)', border: '1px solid #e5e7eb' }}>
                <h3 style={{ fontSize: '18px', fontWeight: 'bold', marginBottom: '10px', borderBottom: '1px solid #eee', paddingBottom: '8px' }}>
                    🏆 Bảng Xếp Hạng (Top 10)
                </h3>
                <div style={{ maxHeight: '200px', overflowY: 'auto' }}>
                    {leaderboard.length === 0 ? (
                        <p style={{ color: '#9ca3af', textAlign: 'center', fontStyle: 'italic', padding: '10px' }}>
                            Chưa có ai ghi danh. Hãy là người đầu tiên!
                        </p>
                    ) : (
                        <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
                            {leaderboard.map((item, idx) => (
                                <li key={item.id || idx} style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #f3f4f6' }}>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                        <span style={{ fontWeight: 'bold', color: idx === 0 ? '#eab308' : (idx === 1 ? '#9ca3af' : (idx === 2 ? '#d97706' : '#6b7280')) }}>#{idx + 1}</span>
                                        {item.avatarUrl ? (
                                            <img src={item.avatarUrl} alt="avt" style={{ width: '30px', height: '30px', borderRadius: '50%', objectFit: 'cover' }} />
                                        ) : (
                                            <div style={{ width: '30px', height: '30px', borderRadius: '50%', backgroundColor: '#e0f2fe', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '12px', fontWeight: 'bold', color: '#0284c7' }}>
                                                {item.username?.charAt(0) || 'U'}
                                            </div>
                                        )}
                                        <span style={{ fontSize: '14px', fontWeight: '500' }}>{item.username}</span>
                                    </div>
                                    <span style={{ fontWeight: 'bold', color: '#16a34a' }}>{item.score}</span>
                                </li>
                            ))}
                        </ul>
                    )}
                </div>
            </div>
        </div>
    );
};

export default SnakeGame;