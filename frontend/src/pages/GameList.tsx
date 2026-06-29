import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Box, Card, CardContent, CardMedia, Typography, Button, Container, Chip, CircularProgress, Divider
} from '@mui/material';
import Grid from '@mui/material/Grid'; // Import Grid chuẩn của MUI v7
import { SportsEsports, VideogameAsset, AutoAwesome, Diamond as DiamondIcon } from '@mui/icons-material';

import api from '../api/api';
import { showWarning, showSuccess } from '../utils/swal';
import GachaBox from '../components/gacha/GachaBox';
import type { User } from '../types';
import type { CosmeticItem } from '../types/cosmetic';

const GAMES = [
    {
        id: 'snake',
        title: 'Rắn Săn Mồi',
        description: 'Trò chơi kinh điển. Điều khiển rắn ăn mồi và tránh va chạm!',
        image: 'https://s3-api.fpt.vn/fptvn-storage/2025-12-01/1764574515_tro-ran-san-moi-tren-google-11.jpg',
        path: '/games/snake',
        category: 'Arcade',
        status: 'Active'
    },
    {
        id: 'tictactoe',
        title: 'Tic Tac Toe Online',
        description: 'Thách đấu bạn bè trong trận chiến cờ ca-rô 3x3 kinh điển, thời gian thực!',
        image: 'https://imgs.search.brave.com/hJXtzJelyBX5YhTf-NiKIwYMFJMq50T12SmGrDuYauI/rs:fit:860:0:0:0/g:ce/aHR0cHM6Ly9tZWRp/YS5nZXR0eWltYWdl/cy5jb20vaWQvMjAz/MjUzNzY4MC9waG90/by90aWMtdGFjLXRv/ZS1nYW1lLW9uLW9y/YW5nZS5qcGc_cz02/MTJ4NjEyJnc9MCZr/PTIwJmM9eE8xMHJI/eElxZWhjMFNsNENY/T2hvNlp0Y3dxRjF0/MFdVcTFMRGNqNjdM/QT0',
        path: '/games/tic-tac-toe',
        category: 'Multiplayer',
        status: 'Active'
    },
    // ... coming soon
];

const GameList: React.FC = () => {
    const navigate = useNavigate();
    const [loadingId, setLoadingId] = useState<string | null>(null);
    const [user, setUser] = useState<User | null>(null);

    // Fetch thông tin user để lấy Avatar, Tên và số điểm hiện tại
    useEffect(() => {
        const fetchUser = async () => {
            try {
                const res = await api.get('/api/auth/profile');
                setUser(res.data);
            } catch (error) {
                console.error("Lỗi khi tải thông tin user", error);
            }
        };
        fetchUser();
    }, []);

    const handlePlayGame = async (game: typeof GAMES[0]) => {
        if (game.id === 'tictactoe') {
            showWarning("Trò chơi này cần 2 người! Hãy vào khung Chat để gửi lời mời cho bạn bè nhé.");
            navigate('/chat');
        } else {
            navigate(game.path);
        }
    };

    const handleGachaSuccess = (wonItem: CosmeticItem, remainingPoints: number) => {
        // Cập nhật lại điểm của user trên giao diện sau khi quay thành công
        if (user) {
            setUser({ ...user, vptlPoints: remainingPoints });
        }
        showSuccess(`Bạn đã nhận được ${wonItem.name}! Vật phẩm đã được chuyển vào Túi đồ.`);
    };

    // Xử lý tên rút gọn cho GachaBox (chỉ lấy tên cuối)
    const previewName = user?.fullName?.split(' ').pop() || 'Tên';

    return (
        <Container maxWidth="lg" sx={{ py: 4 }}>
            
            {/* --- SECTION 1: SỰ KIỆN GACHA MÙA HÈ --- */}
            <Box sx={{ mb: 6 }}>
                {/* Header Sự Kiện + Hiển thị số điểm */}
                <Box sx={{ 
                    display: 'flex', 
                    justifyContent: 'space-between', 
                    alignItems: 'center', 
                    mb: 3, 
                    flexWrap: 'wrap', 
                    gap: 2 
                }}>
                    <Typography variant="h4" sx={{ fontWeight: 'bold', color: '#f59e0b', display: 'flex', alignItems: 'center', gap: 1 }}>
                        <AutoAwesome fontSize="large" /> Sự Kiện Đặc Biệt
                    </Typography>
                    
                    {/* Box hiển thị điểm VPTL */}
                    <Box sx={{ 
                        display: 'flex', 
                        alignItems: 'center', 
                        gap: 1, 
                        background: 'rgba(245, 166, 35, 0.1)', // Đổi màu nền nhè nhẹ tone cam để hợp mọi theme
                        border: '1px solid rgba(245, 166, 35, 0.3)',
                        px: 3, 
                        py: 1.5, 
                        borderRadius: '50px' 
                    }}>
                        <DiamondIcon sx={{ color: '#F5A623', fontSize: 32 }} />
                        <Typography variant="h5" fontWeight="bold" color="#F5A623">
                            {user?.vptlPoints || 0}
                        </Typography>
                    </Box>
                </Box>
                
                <Grid container spacing={4} justifyContent="center">
                    <Grid size={{ xs: 12, md: 8, lg: 6 }}>
                        <GachaBox 
                            onSuccess={handleGachaSuccess}
                            userAvatarUrl={user?.avatarUrl || undefined}
                            userName={previewName}
                        />
                    </Grid>
                </Grid>
            </Box>

            <Divider sx={{ my: 4, borderColor: 'rgba(255,255,255,0.1)' }} />

            {/* --- SECTION 2: DANH SÁCH GAME --- */}
            <Box sx={{ mb: 4, textAlign: 'center' }}>
                <Typography variant="h3" sx={{ fontWeight: 'bold', color: '#16a34a', mb: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 2 }}>
                    <SportsEsports fontSize="large" /> Game Center
                </Typography>
                <Typography variant="subtitle1" color="text.secondary">
                    Kiếm VPTL Points để mở rương bằng cách chơi game cùng bạn bè!
                </Typography>
            </Box>

            <Grid container spacing={4}>
                {GAMES.map((game) => (
                    <Grid key={game.id} size={{ xs: 12, sm: 6, md: 4 }}>
                        <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column', transition: 'transform 0.2s', '&:hover': { transform: 'translateY(-5px)', boxShadow: 6 } }}>
                            <CardMedia component="img" height="200" image={game.image} alt={game.title} />
                            <CardContent sx={{ flexGrow: 1 }}>
                                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                                    <Typography gutterBottom variant="h5" fontWeight="bold">{game.title}</Typography>
                                    <Chip label={game.category} size="small" color={game.status === 'Active' ? 'success' : 'default'} variant="outlined" />
                                </Box>
                                <Typography variant="body2" color="text.secondary">{game.description}</Typography>
                            </CardContent>
                            <Box sx={{ p: 2, pt: 0 }}>
                                <Button
                                    variant="contained"
                                    fullWidth
                                    color={game.status === 'Active' ? 'success' : 'inherit'}
                                    disabled={game.status !== 'Active' || loadingId === game.id}
                                    onClick={() => handlePlayGame(game)}
                                    startIcon={loadingId === game.id ? <CircularProgress size={20} color="inherit" /> : <VideogameAsset />}
                                >
                                    {loadingId === game.id ? 'Đang tạo phòng...' : (game.status === 'Active' ? 'Chơi Ngay' : 'Sắp Ra Mắt')}
                                </Button>
                            </Box>
                        </Card>
                    </Grid>
                ))}
            </Grid>
        </Container>
    );
};

export default GameList;