import React, { useState, useEffect } from 'react';
import { 
  Box, Typography, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, 
  IconButton, Avatar, Chip, Tooltip, CircularProgress
} from '@mui/material';
import BlockIcon from '@mui/icons-material/Block';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import SchoolIcon from '@mui/icons-material/School';
import SupervisorAccountIcon from '@mui/icons-material/SupervisorAccount'; // ⭐️ Import thêm icon cho Giảng viên

import api from '../../api/api'; 

interface User {
  id: number;
  studentCode: string;
  fullName: string;
  email: string;
  className?: string;
  role: string;
  avatarUrl?: string;
  active: boolean;
  lastLogin?: string;
}

export const UserManager: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
        setLoading(true);
        const response = await api.get('/api/admin/users');
        setUsers(response.data);
    } catch (error) {
        console.error("Lỗi tải danh sách người dùng", error);
    } finally {
        setLoading(false);
    }
  };

  const handleApproveUser = async (userId: number) => {
    try {
        await api.post(`/api/admin/approve-user/${userId}`);
        fetchUsers(); 
    } catch (error) {
        alert("Lỗi khi duyệt người dùng.");
    }
  };

  const handleBanUser = async (userId: number) => {
    const confirm = window.confirm("Bạn có chắc chắn muốn khóa tài khoản này?");
    if(confirm) {
        try {
            await api.post(`/api/admin/ban-user/${userId}`);
            fetchUsers();
        } catch (error) {
            alert("Lỗi khi khóa người dùng.");
        }
    }
  };

  // ⭐️ Hàm hỗ trợ render Role Chip
  const renderRoleChip = (role: string) => {
    switch (role) {
        case 'ADMIN':
            return <Chip icon={<AdminPanelSettingsIcon />} label="Admin" color="error" size="small" variant="outlined" />;
        case 'TEACHER': // ⭐️ Xử lý role Giảng viên
            return <Chip icon={<SupervisorAccountIcon />} label="Giảng viên" color="primary" size="small" variant="outlined" />;
        default: // STUDENT hoặc các role khác
            return <Chip icon={<SchoolIcon />} label="Sinh viên" color="default" size="small" variant="outlined" />;
    }
  };

  if (loading) return <Box sx={{ p: 3, textAlign: 'center' }}><CircularProgress /></Box>;

  return (
    <Box>
       <Typography variant="h5" sx={{ mb: 3, fontWeight: 'bold' }}>Quản lý Người dùng</Typography>
       
       <TableContainer component={Paper} elevation={2}>
        <Table>
          <TableHead sx={{ bgcolor: '#f5f5f5' }}>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Họ tên</TableCell>
              <TableCell>Mã số</TableCell>
              <TableCell>Email</TableCell>
              <TableCell>Vai trò</TableCell>
              <TableCell>Trạng thái</TableCell>
              <TableCell align="right">Hành động</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
             {users.length > 0 ? users.map((user) => (
               <TableRow key={user.id} hover>
                 <TableCell>{user.id}</TableCell>
                 
                 <TableCell>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Avatar src={user.avatarUrl} alt={user.fullName} />
                        <Box>
                            <Typography variant="body2" fontWeight="bold">{user.fullName}</Typography>
                            <Typography variant="caption" color="text.secondary">{user.className}</Typography>
                        </Box>
                    </Box>
                 </TableCell>

                 <TableCell>{user.studentCode}</TableCell>
                 <TableCell>{user.email}</TableCell>
                 
                 {/* ⭐️ Cột Vai trò: Gọi hàm render đã viết lại logic */}
                 <TableCell>
                    {renderRoleChip(user.role)}
                 </TableCell>

                 <TableCell>
                    <Chip 
                        label={user.active ? "Hoạt động" : "Chờ duyệt / Khóa"} 
                        color={user.active ? 'success' : 'error'}
                        size="small"
                        sx={{ fontWeight: 'bold' }}
                    />
                 </TableCell>

                 <TableCell align="right">
                    {!user.active ? (
                        <Tooltip title="Duyệt / Mở khóa">
                            <IconButton size="small" color="success" onClick={() => handleApproveUser(user.id)}>
                                <CheckCircleIcon />
                            </IconButton>
                        </Tooltip>
                    ) : (
                        <Tooltip title="Khóa tài khoản">
                            <IconButton size="small" color="error" onClick={() => handleBanUser(user.id)}>
                                <BlockIcon />
                            </IconButton>
                        </Tooltip>
                    )}
                 </TableCell>
               </TableRow>
             )) : (
                <TableRow>
                    <TableCell colSpan={7} align="center" sx={{ py: 3 }}>
                        <Typography variant="body2" color="textSecondary">
                            Không tìm thấy dữ liệu.
                        </Typography>
                    </TableCell>
                </TableRow>
             )}
          </TableBody>
        </Table>
       </TableContainer>
    </Box>
  );
};