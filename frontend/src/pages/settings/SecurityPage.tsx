import { useState, useEffect } from 'react';
import {
    Box, Container, Typography, Paper, Table, TableBody,
    TableCell, TableContainer, TableHead, TableRow, Chip, CircularProgress,
    Button, Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions
} from '@mui/material';
import ComputerIcon from '@mui/icons-material/Computer';
import SmartphoneIcon from '@mui/icons-material/Smartphone';
import api from '../../api/api'; // Đường dẫn import file api.ts của bạn

// 🟢 BỔ SUNG THÊM 2 TRƯỜNG isActive VÀ isCurrentDevice
interface SecurityHistory {
    id: number;
    ipAddress: string;
    browser: string;
    device: string;
    loginTime: string;
    status: string;
    isActive: boolean;
    isCurrentDevice: boolean;
}

export default function SecurityPage() {
    const [historyList, setHistoryList] = useState<SecurityHistory[]>([]);
    const [loading, setLoading] = useState<boolean>(true);

    // State cho Popup xác nhận đăng xuất
    const [openConfirm, setOpenConfirm] = useState(false);
    const [selectedId, setSelectedId] = useState<number | null>(null);

    useEffect(() => {
        fetchHistory();
    }, []);

    const fetchHistory = async () => {
        try {
            const res = await api.get('/api/auth/security-history');
            setHistoryList(res.data);
        } catch (error) {
            console.error("Lỗi lấy lịch sử bảo mật:", error);
        } finally {
            setLoading(false);
        }
    };

    // 🟢 HÀM XỬ LÝ KHI BẤM NÚT ĐĂNG XUẤT
    const handleRevokeClick = (id: number) => {
        setSelectedId(id);
        setOpenConfirm(true);
    };

    const confirmRevoke = async () => {
        if (selectedId === null) return;

        try {
            await api.post(`/api/auth/security-history/${selectedId}/revoke`);

            // Cập nhật lại UI ngay lập tức mà không cần gọi lại API
            setHistoryList(prevList =>
                prevList.map(item =>
                    item.id === selectedId ? { ...item, isActive: false } : item
                )
            );
        } catch (error) {
            console.error("Lỗi đăng xuất thiết bị:", error);
            alert("Không thể đăng xuất thiết bị lúc này.");
        } finally {
            setOpenConfirm(false);
            setSelectedId(null);
        }
    };
    // 🟢 HÀM FORMAT THỜI GIAN
    const formatTime = (isoString: string) => {
        if (!isoString) return '';
        const date = new Date(isoString);
        return date.toLocaleString('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
        });
    };

    return (
        <Container maxWidth="lg">
            <Box sx={{ mb: 4, mt: 2 }}>
                <Typography variant="h5" fontWeight="bold" gutterBottom>
                    Nơi bạn đã đăng nhập
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    Đây là danh sách các thiết bị đã đăng nhập vào tài khoản của bạn. Hãy đăng xuất khỏi những thiết bị mà bạn không nhận ra.
                </Typography>
            </Box>

            <TableContainer component={Paper} elevation={2} sx={{ borderRadius: 2 }}>
                {loading ? (
                    <Box sx={{ display: 'flex', justifyContent: 'center', p: 5 }}>
                        <CircularProgress />
                    </Box>
                ) : (
                    <Table sx={{ minWidth: 700 }} aria-label="security history table">
                        <TableHead sx={{ bgcolor: '#F0F2F5' }}>
                            <TableRow>
                                <TableCell sx={{ fontWeight: 'bold' }}>Thiết bị</TableCell>
                                <TableCell sx={{ fontWeight: 'bold' }}>Thời gian / Địa điểm</TableCell>
                                <TableCell sx={{ fontWeight: 'bold' }}>Trạng thái đăng nhập</TableCell>
                                <TableCell align="right" sx={{ fontWeight: 'bold' }}>Hành động</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {historyList.length > 0 ? (
                                historyList.map((row) => (
                                    <TableRow
                                        key={row.id}
                                        sx={{
                                            '&:last-child td, &:last-child th': { border: 0 },
                                            // Làm mờ row nếu thiết bị đã bị đăng xuất
                                            opacity: row.isActive ? 1 : 0.6
                                        }}
                                    >
                                        {/* CỘT 1: ICON VÀ THÔNG TIN THIẾT BỊ */}
                                        <TableCell>
                                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                                                {row.device.toLowerCase().includes('ios') || row.device.toLowerCase().includes('android') ? (
                                                    <SmartphoneIcon sx={{ color: row.isActive ? 'primary.main' : 'text.secondary', fontSize: 32 }} />
                                                ) : (
                                                    <ComputerIcon sx={{ color: row.isActive ? 'primary.main' : 'text.secondary', fontSize: 32 }} />
                                                )}
                                                <Box>
                                                    <Typography variant="body1" fontWeight={row.isCurrentDevice ? "bold" : "normal"}>
                                                        {row.device}
                                                    </Typography>
                                                    <Typography variant="caption" color="text.secondary">
                                                        {row.browser}
                                                    </Typography>
                                                </Box>
                                            </Box>
                                        </TableCell>

                                        {/* CỘT 2: THỜI GIAN VÀ IP */}
                                        <TableCell>
                                            {/* 🟢 SỬA LẠI DÒNG NÀY: Bọc row.loginTime bằng formatTime() */}
                                            <Typography variant="body2">{formatTime(row.loginTime)}</Typography>
                                            <Typography variant="caption" color="text.secondary">IP: {row.ipAddress}</Typography>
                                        </TableCell>
                                        {/* CỘT 3: TRẠNG THÁI HIỆN TẠI */}
                                        <TableCell>
                                            {row.isCurrentDevice ? (
                                                <Typography variant="body2" color="success.main" fontWeight="bold">
                                                    Đang hoạt động
                                                </Typography>
                                            ) : !row.isActive ? (
                                                <Typography variant="body2" color="text.secondary">
                                                    Đã đăng xuất
                                                </Typography>
                                            ) : (
                                                <Typography variant="body2" color="text.primary">
                                                    Đang đăng nhập
                                                </Typography>
                                            )}
                                        </TableCell>

                                        {/* CỘT 4: NÚT ĐĂNG XUẤT */}
                                        <TableCell align="right">
                                            {!row.isCurrentDevice && row.isActive && (
                                                <Button
                                                    variant="outlined"
                                                    color="error"
                                                    size="small"
                                                    onClick={() => handleRevokeClick(row.id)}
                                                    sx={{ textTransform: 'none', borderRadius: 2 }}
                                                >
                                                    Đăng xuất
                                                </Button>
                                            )}
                                        </TableCell>
                                    </TableRow>
                                ))
                            ) : (
                                <TableRow>
                                    <TableCell colSpan={4} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                                        Chưa có dữ liệu lịch sử đăng nhập.
                                    </TableCell>
                                </TableRow>
                            )}
                        </TableBody>
                    </Table>
                )}
            </TableContainer>

            {/* POPUP XÁC NHẬN */}
            <Dialog open={openConfirm} onClose={() => setOpenConfirm(false)}>
                <DialogTitle sx={{ fontWeight: 'bold' }}>Xác nhận đăng xuất</DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Bạn có chắc chắn muốn đăng xuất tài khoản khỏi thiết bị này không? Bạn sẽ cần đăng nhập lại nếu muốn sử dụng thiết bị đó.
                    </DialogContentText>
                </DialogContent>
                <DialogActions sx={{ px: 3, pb: 2 }}>
                    <Button onClick={() => setOpenConfirm(false)} color="inherit" sx={{ textTransform: 'none' }}>
                        Hủy
                    </Button>
                    <Button onClick={confirmRevoke} variant="contained" color="error" sx={{ textTransform: 'none' }}>
                        Đăng xuất
                    </Button>
                </DialogActions>
            </Dialog>
        </Container>
    );
}