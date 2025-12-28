import { useEffect, useState } from 'react';
import api from '../../api/api';

import {
  Card,
  CardContent,
  Typography,
  Box,
  CircularProgress,
} from '@mui/material';

import Grid from '@mui/material/Grid';

import ArticleIcon from '@mui/icons-material/Article';
import PeopleIcon from '@mui/icons-material/People';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';

/* ================= TYPES ================= */
interface DashboardStats {
  totalPosts: number;
  activeUsers: number;
  pendingUsers: number;
}

/* ================= COMPONENT ================= */
const AdminDashboard = () => {
  const [stats, setStats] = useState<DashboardStats>({
    totalPosts: 0,
    activeUsers: 0,
    pendingUsers: 0,
  });

  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDashboardStats();
  }, []);

  const fetchDashboardStats = async () => {
    try {
      const [postRes, userRes] = await Promise.allSettled([
        api.get<number>('/api/admin/posts-count'),
        api.get<{ activeUsers: number; pendingUsers: number }>(
          '/api/admin/users-stats'
        ),
      ]);

      setStats({
        totalPosts:
          postRes.status === 'fulfilled' ? postRes.value.data : 0,
        activeUsers:
          userRes.status === 'fulfilled'
            ? userRes.value.data.activeUsers
            : 0,
        pendingUsers:
          userRes.status === 'fulfilled'
            ? userRes.value.data.pendingUsers
            : 0,
      });
    } catch (error) {
      console.error('Lỗi tải dashboard:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}>
        <CircularProgress />
      </Box>
    );
  }

  /* ================= STAT CARD ================= */
  const StatCard = ({
    title,
    value,
    icon,
    color,
  }: {
    title: string;
    value: number;
    icon: React.ReactNode;
    color: string;
  }) => (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Box
          sx={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}
        >
          <Box>
            <Typography variant="overline" color="text.secondary">
              {title}
            </Typography>
            <Typography variant="h4" fontWeight="bold">
              {value}
            </Typography>
          </Box>

          <Box
            sx={{
              p: 1.5,
              borderRadius: '50%',
              bgcolor: `${color}20`,
              color,
            }}
          >
            {icon}
          </Box>
        </Box>
      </CardContent>
    </Card>
  );

  /* ================= RENDER ================= */
  return (
    <Box>
      <Typography variant="h4" sx={{ mb: 4, fontWeight: 'bold' }}>
        Tổng quan hệ thống
      </Typography>

      <Grid container spacing={3}>
        <Box sx={{ width: { xs: '100%', md: '50%' }, p: 1 }}>
          <StatCard
            title="TỔNG BÀI VIẾT"
            value={stats.totalPosts}
            icon={<ArticleIcon fontSize="large" />}
            color="#1976d2"
          />
        </Box>

        <Box sx={{ width: { xs: '100%', md: '50%' }, p: 1 }}>
          <StatCard
            title="NGƯỜI DÙNG CHỜ DUYỆT"
            value={stats.pendingUsers}
            icon={<PeopleIcon fontSize="large" />}
            color="#ed6c02"
          />
        </Box>

        <Box sx={{ width: { xs: '100%', md: '50%' }, p: 1 }}>
          <StatCard
            title="NGƯỜI DÙNG HOẠT ĐỘNG"
            value={stats.activeUsers}
            icon={<VerifiedUserIcon fontSize="large" />}
            color="#2e7d32"
          />
        </Box>
      </Grid>
    </Box>
  );
};

export default AdminDashboard;
