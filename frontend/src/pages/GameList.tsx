import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import {
    Box, Grid, Card, CardContent, CardMedia, Typography, Button, Container, Chip, CircularProgress
} from '@mui/material';
import { showWarning } from '../utils/swal';
import { SportsEsports, VideogameAsset } from '@mui/icons-material';

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
        path: '/games/tic-tac-toe', // Đây là tiền tố đường dẫn
        category: 'Multiplayer',
        status: 'Active'
    },
    // ... coming soon
];

const GameList: React.FC = () => {
    const navigate = useNavigate();
    const [loadingId, setLoadingId] = useState<string | null>(null);
    const token = localStorage.getItem('token');

    const handlePlayGame = async (game: typeof GAMES[0]) => {
        if (game.id === 'tictactoe') {
            // Cách 1: Hiện thông báo và chuyển hướng sang trang Chat
            showWarning("Trò chơi này cần 2 người! Hãy vào khung Chat để gửi lời mời cho bạn bè nhé.");
            navigate('/chat'); // Thay bằng route trang chat của bạn

            // Cách 2 (Nâng cao): Mở một Modal popup hiển thị danh sách bạn bè online để chọn mời ngay tại đây.
        } else {
            navigate(game.path);
        }
    };

    return (
        <Container maxWidth="lg" sx={{ py: 4 }}>
            <Box sx={{ mb: 4, textAlign: 'center' }}>
                <Typography variant="h3" sx={{ fontWeight: 'bold', color: '#16a34a', mb: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 2 }}>
                    <SportsEsports fontSize="large" /> Game Center
                </Typography>
                <Typography variant="subtitle1" color="text.secondary">
                    Giải trí và đua top cùng bạn bè sau những giờ học căng thẳng
                </Typography>
            </Box>

            <Grid container spacing={4}>
                {GAMES.map((game) => (
                    <Grid item key={game.id} xs={12} sm={6} md={4}>
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