import { useState, useEffect } from 'react';
import {
    Box, Container, Typography, Paper, Table, TableBody,
    TableCell, TableContainer, TableHead, TableRow, CircularProgress,
    Button, Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions,
    TablePagination // 🟢 IMPORT THÊM BỘ PHÂN TRANG
} from '@mui/material';
import ComputerIcon from '@mui/icons-material/Computer';
import SmartphoneIcon from '@mui/icons-material/Smartphone';
import api from '../../api/api'; 

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

    // 🟢 STATE CHO PHÂN TRANG DỮ LIỆU LỚN
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(5);
    const [totalElements, setTotalElements] = useState(0);

    // State cho Popup xác nhận đăng xuất
    const [openConfirm, setOpenConfirm] = useState(false);
    const [selectedId, setSelectedId] = useState<number | null>(null);

    useEffect(() => {
        fetchHistory(page, rowsPerPage);
    }, []);

    // 🟢 HÀM FETCH ĐÃ ĐƯỢC TỐI ƯU CÓ THAM SỐ PHÂN TRANG
    const fetchHistory = async (currentPage: number, currentSize: number) => {
        setLoading(true);
        try {
            // Truyền page và size xuống Backend để DB chỉ lấy đúng số dòng cần thiết
            const res = await api.get(`/api/auth/security-history?page=${currentPage}&size=${currentSize}`);
            
            // Xử lý dữ liệu trả về theo chuẩn Page<T> của Spring Boot
            const data = res.data;
            const content = data?.content || data?.data || (Array.isArray(data) ? data : []);
            
            setHistoryList(content);
            // Lấy tổng số record từ DB để vẽ thanh phân trang
            setTotalElements(data?.totalElements || data?.data?.totalElements || content.length);
        } catch (error) {
            console.error("Lỗi lấy lịch sử bảo mật:", error);
        } finally {
            setLoading(false);
        }
    };

    // 🟢 SỰ KIỆN KHI CHUYỂN TRANG HOẶC ĐỔI SỐ DÒNG HIỂN THỊ
    const handleChangePage = (event: unknown, newPage: number) => {
        setPage(newPage);
        fetchHistory(newPage, rowsPerPage);
    };

    const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
        const newSize = parseInt(event.target.value, 10);
        setRowsPerPage(newSize);
        setPage(0); // Quay về trang đầu tiên
        fetchHistory(0, newSize);
    };

    const handleRevokeClick = (id: number) => {
        setSelectedId(id);
        setOpenConfirm(true);
    };

    const confirmRevoke = async () => {
        if (selectedId === null) return;
        try {
            await api.post(`/api/auth/security-history/${selectedId}/revoke`);
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

    const formatTime = (isoString: string) => {
        if (!isoString) return '';
        const date = new Date(isoString);
        return date.toLocaleString('vi-VN', {
            day: '2-digit', month: '2-digit', year: 'numeric',
            hour: '2-digit', minute: '2-digit', second: '2-digit',
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
                    <>
                        <Table sx={{ minWidth: 700 }} aria-label="security history table">
                            {/* 🟢 ĐÃ SỬA: Đổi bgcolor thành action.hover để tự động thay đổi theo Giao diện Sáng/Tối */}
                            <TableHead sx={{ bgcolor: 'action.hover' }}>
                                <TableRow>
                                    <TableCell sx={{ fontWeight: 'bold', color: 'text.primary' }}>Thiết bị</TableCell>
                                    <TableCell sx={{ fontWeight: 'bold', color: 'text.primary' }}>Thời gian / Địa điểm</TableCell>
                                    <TableCell sx={{ fontWeight: 'bold', color: 'text.primary' }}>Trạng thái đăng nhập</TableCell>
                                    <TableCell align="right" sx={{ fontWeight: 'bold', color: 'text.primary' }}>Hành động</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {historyList.length > 0 ? (
                                    historyList.map((row) => (
                                        <TableRow
                                            key={row.id}
                                            sx={{
                                                '&:last-child td, &:last-child th': { border: 0 },
                                                opacity: row.isActive ? 1 : 0.6
                                            }}
                                        >
                                            <TableCell>
                                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                                                    {row.device.toLowerCase().includes('ios') || row.device.toLowerCase().includes('android') ? (
                                                        <SmartphoneIcon sx={{ color: row.isActive ? 'primary.main' : 'text.disabled', fontSize: 32 }} />
                                                    ) : (
                                                        <ComputerIcon sx={{ color: row.isActive ? 'primary.main' : 'text.disabled', fontSize: 32 }} />
                                                    )}
                                                    <Box>
                                                        <Typography variant="body1" fontWeight={row.isCurrentDevice ? "bold" : "normal"} color="text.primary">
                                                            {row.device}
                                                        </Typography>
                                                        <Typography variant="caption" color="text.secondary">
                                                            {row.browser}
                                                        </Typography>
                                                    </Box>
                                                </Box>
                                            </TableCell>
                                            <TableCell>
                                                <Typography variant="body2" color="text.primary">{formatTime(row.loginTime)}</Typography>
                                                <Typography variant="caption" color="text.secondary">IP: {row.ipAddress}</Typography>
                                            </TableCell>
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
                        
                        {/* 🟢 GẮN THANH PHÂN TRANG VÀO DƯỚI CÙNG */}
                        <TablePagination
                            rowsPerPageOptions={[5, 10, 25]}
                            component="div"
                            count={totalElements}
                            rowsPerPage={rowsPerPage}
                            page={page}
                            onPageChange={handleChangePage}
                            onRowsPerPageChange={handleChangeRowsPerPage}
                            labelRowsPerPage="Số dòng mỗi trang:"
                            labelDisplayedRows={({ from, to, count }) => `${from}–${to} trên ${count !== -1 ? count : `hơn ${to}`}`}
                        />
                    </>
                )}
            </TableContainer>

            {/* POPUP XÁC NHẬN GIỮ NGUYÊN */}
            <Dialog open={openConfirm} onClose={() => setOpenConfirm(false)}>
                <DialogTitle sx={{ fontWeight: 'bold' }}>Xác nhận đăng xuất</DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Bạn có chắc chắn muốn đăng xuất tài khoản khỏi thiết bị này không? Bạn sẽ cần đăng nhập lại nếu muốn sử dụng thiết bị đó.
                    </DialogContentText>
                </DialogContent>
                <DialogActions sx={{ px: 3, pb: 2 }}>
                    <Button onClick={() => setOpenConfirm(false)} color="inherit" sx={{ textTransform: 'none' }}>Hủy</Button>
                    <Button onClick={confirmRevoke} variant="contained" color="error" sx={{ textTransform: 'none' }}>Đăng xuất</Button>
                </DialogActions>
            </Dialog>
        </Container>
    );
}