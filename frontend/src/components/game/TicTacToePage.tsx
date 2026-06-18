import React, { useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
    Container, Box, Typography, Button, Paper, Grid, Badge, Chip, CircularProgress,
    Dialog, DialogTitle, DialogContent, DialogActions, Zoom
} from '@mui/material';
import { ArrowBack, PlayArrow, Person, EmojiEvents, SentimentDissatisfied, Handshake } from '@mui/icons-material';
import { useWebSocket } from '../../context/useWebSocket';
import { useAuth } from '../../context/AuthContext'; 

const TicTacToePage: React.FC = () => {
    const { sessionId } = useParams<{ sessionId?: string }>();
    const navigate = useNavigate();
    const { currentGame, sendGameAction, subscribeToRoom, isConnected } = useWebSocket();
    const { user } = useAuth(); 

    useEffect(() => {
        if (sessionId && isConnected) {
            subscribeToRoom(Number(sessionId));
            sendGameAction('game.room.join', { sessionId: Number(sessionId) });
        }
    }, [sessionId, isConnected]);

    const handleCellClick = (row: number, col: number) => {
        if (!currentGame || currentGame.status !== 'PLAYING') return;
        
        const cell = currentGame.board[row][col];
        if (cell === 'X' || cell === 'O') return;

        sendGameAction('game.move', {
            sessionId: currentGame.id,
            row,
            col
        });
    };

    const handleStartGame = () => {
        if (currentGame) {
            sendGameAction('game.start', { sessionId: currentGame.id });
        }
    };

    if (!isConnected) {
        return (
            <Box display="flex" justifyContent="center" alignItems="center" height="80vh">
                <CircularProgress color="success" />
                <Typography sx={{ ml: 2, fontWeight: 'bold' }}>Đang kết nối đấu trường...</Typography>
            </Box>
        );
    }

    const isHost = currentGame?.hostId === user?.id;
    const isMyTurn = currentGame?.currentTurnId === user?.id;

    const isFinished = currentGame?.status === 'FINISHED';
    const isWinner = currentGame?.winnerId === user?.id;
    const isDraw = isFinished && !currentGame?.winnerId;
    const isLoser = isFinished && !isWinner && !isDraw;

    return (
        <Container maxWidth="md" sx={{ py: 4 }}>
            <Box sx={{ mb: 4, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Button startIcon={<ArrowBack />} onClick={() => navigate('/games')} color="inherit" sx={{ fontWeight: 'bold' }}>
                    Rời phòng
                </Button>
                <Chip 
                    label={currentGame?.status === 'PLAYING' ? 'ĐANG CHIẾN ĐẤU' : currentGame?.status || 'WAITING'} 
                    color={currentGame?.status === 'PLAYING' ? 'error' : 'warning'} 
                    sx={{ fontWeight: 'bold', px: 2 }}
                />
            </Box>

            <Grid container spacing={4}>
                <Grid item xs={12} md={4}>
                    <Paper elevation={4} sx={{ p: 3, textAlign: 'center', borderRadius: 4, mb: 2 }}>
                        <Typography variant="h6" fontWeight="bold" gutterBottom>Khu vực chờ</Typography>
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 2 }}>
                            <Badge badgeContent="Host" color="error">
                                <Chip 
                                    icon={<Person />} 
                                    label={`Player: ${currentGame?.hostId || '...'}`} 
                                    variant={user?.id === currentGame?.hostId ? "filled" : "outlined"}
                                    color={user?.id === currentGame?.hostId ? "primary" : "default"}
                                    sx={{ width: '100%', py: 2, fontSize: '1.1rem' }} 
                                />
                            </Badge>
                            
                            <Typography variant="h6" color="text.secondary" fontWeight="bold">VS</Typography>
                            
                            <Chip 
                                icon={<Person />} 
                                label={currentGame?.guestId ? `Player: ${currentGame.guestId}` : 'Chờ đối thủ...'} 
                                variant={user?.id === currentGame?.guestId ? "filled" : "outlined"}
                                color={user?.id === currentGame?.guestId ? "secondary" : "default"}
                                sx={{ py: 2, fontSize: '1.1rem' }} 
                            />
                        </Box>

                        {currentGame?.status === 'WAITING' && (
                            <Zoom in={true}>
                                <Button 
                                    variant="contained" 
                                    color="success" 
                                    fullWidth 
                                    sx={{ mt: 4, py: 1.5, fontSize: '1.1rem', fontWeight: 'bold', borderRadius: 2 }}
                                    startIcon={<PlayArrow />}
                                    onClick={handleStartGame}
                                    disabled={!currentGame.guestId || !isHost} 
                                >
                                    {isHost ? 'BẮT ĐẦU TRẬN ĐẤU' : 'ĐỢI HOST BẮT ĐẦU'}
                                </Button>
                            </Zoom>
                        )}
                    </Paper>
                </Grid>

                <Grid item xs={12} md={8}>
                    <Paper elevation={6} sx={{ 
                        p: 3, 
                        bgcolor: '#1a1e29',
                        borderRadius: 6, 
                        aspectRatio: '1/1', 
                        display: 'flex', 
                        alignItems: 'center', 
                        justifyContent: 'center',
                        boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.2)' 
                    }}>
                        {currentGame?.status === 'PLAYING' || currentGame?.status === 'FINISHED' ? (
                            <Box sx={{ 
                                display: 'grid', 
                                gridTemplateColumns: 'repeat(3, 1fr)', 
                                gridTemplateRows: 'repeat(3, 1fr)', 
                                gap: 2, 
                                width: '100%', 
                                height: '100%'
                            }}>
                                {currentGame.board.map((rowArr, rowIndex) => (
                                    rowArr.map((cell, colIndex) => {
                                        const isX = cell === 'X';
                                        const isO = cell === 'O';
                                        const displayMark = isX ? 'X' : isO ? 'O' : '';

                                        return (
                                            <Button
                                                key={`${rowIndex}-${colIndex}`}
                                                variant="contained"
                                                disableElevation 
                                                sx={{ 
                                                    width: '100%',             // 🔴 KHÓA RỘNG
                                                    height: '100%',            // 🔴 KHÓA CAO
                                                    aspectRatio: '1/1',        // 🔴 KHÓA TỶ LỆ VUÔNG
                                                    padding: 0,                // 🔴 XÓA PADDING
                                                    lineHeight: 1,             // 🔴 KHÓA CHIỀU CAO DÒNG
                                                    
                                                    fontSize: '4.5rem', 
                                                    fontWeight: 900,
                                                    fontFamily: '"Nunito", "Segoe UI", sans-serif',
                                                    bgcolor: displayMark ? (isX ? '#ffe4e6' : '#e0f2fe') : '#2a3143', 
                                                    color: isX ? '#e11d48' : '#0284c7',
                                                    borderRadius: '24px', 
                                                    transition: 'background-color 0.3s', // Rút gọn transition, bỏ transform
                                                    textTransform: 'none',
                                                    '&:hover': { 
                                                        bgcolor: displayMark ? (isX ? '#fecdd3' : '#bae6fd') : '#374151',
                                                        // 🔴 KHÔNG ZOOM NÚT KHI HOVER NỮA ĐỂ TRÁNH RUNG LẮC
                                                    }
                                                }}
                                                onClick={() => handleCellClick(rowIndex, colIndex)}
                                            >
                                                {displayMark && (
                                                    <Zoom in={true} timeout={300}>
                                                        <span>{displayMark}</span>
                                                    </Zoom>
                                                )}
                                            </Button>
                                        );
                                    })
                                ))}
                            </Box>
                        ) : (
                            <Typography color="white" variant="h6">Phòng chờ: Đang đợi đối thủ vào bàn...</Typography>
                        )}
                    </Paper>

                    {currentGame?.status === 'PLAYING' && (
                        <Box sx={{ mt: 3, textAlign: 'center' }}>
                            <Chip 
                                label={`Đến lượt: Player ${currentGame.currentTurnId}`} 
                                color={isMyTurn ? "success" : "default"}
                                variant={isMyTurn ? "filled" : "outlined"}
                                sx={{ fontSize: '1.2rem', py: 2.5, px: 2, fontWeight: 'bold' }} 
                            />
                        </Box>
                    )}
                </Grid>
            </Grid>

            {/* POPUP THÔNG BÁO KẾT QUẢ TRẬN ĐẤU */}
            <Dialog 
                open={isFinished} 
                TransitionComponent={Zoom}
                PaperProps={{
                    sx: { 
                        borderRadius: 4, 
                        p: 2, 
                        textAlign: 'center',
                        bgcolor: isWinner ? '#f0fdf4' : isLoser ? '#fef2f2' : '#f8fafc' 
                    }
                }}
            >
                <DialogTitle sx={{ fontSize: '2rem', fontWeight: 'bold', color: isWinner ? '#16a34a' : isLoser ? '#dc2626' : '#475569' }}>
                    {isWinner && <EmojiEvents sx={{ fontSize: 60, color: '#fbbf24', mb: 1 }} />}
                    {isLoser && <SentimentDissatisfied sx={{ fontSize: 60, color: '#f87171', mb: 1 }} />}
                    {isDraw && <Handshake sx={{ fontSize: 60, color: '#94a3b8', mb: 1 }} />}
                    <br/>
                    {isWinner ? 'CHIẾN THẮNG!' : isLoser ? 'THẤT BẠI!' : 'HÒA CỜ!'}
                </DialogTitle>
                
                <DialogContent>
                    <Typography variant="h6" color="text.secondary">
                        {isWinner 
                            ? "Chúc mừng! Bạn đã thể hiện đẳng cấp vô đối! 🎉" 
                            : isLoser 
                                ? "Kẻ thù quá mạnh! Phục thù vào ván sau nhé. ⚔️" 
                                : "Bất phân thắng bại! Hai người đúng là kỳ phùng địch thủ. 🤝"}
                    </Typography>
                </DialogContent>

                <DialogActions sx={{ justifyContent: 'center', mt: 2 }}>
                    <Button 
                        variant="contained" 
                        size="large"
                        onClick={() => navigate('/games')} 
                        sx={{ borderRadius: 2, fontWeight: 'bold', px: 4 }}
                    >
                        Trở về Lobby
                    </Button>
                </DialogActions>
            </Dialog>

        </Container>
    );
};

export default TicTacToePage;