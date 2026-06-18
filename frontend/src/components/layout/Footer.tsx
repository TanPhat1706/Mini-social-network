import React from 'react';
import { Box, Container, Typography, Link, Grid, Divider } from '@mui/material';

export default function Footer() {
  return (
    <Box component="footer" sx={{ py: 6, px: 2, mt: 'auto', backgroundColor: '#FFFFFF', borderTop: '1px solid #dddfe2' }}>
      <Container maxWidth="lg">
        <Grid container spacing={4} justifyContent="space-between">
          <Grid item xs={12} sm={4}>
            <Typography variant="h6" color="primary" fontWeight={700} gutterBottom>MiniSocial Khoa</Typography>
            <Typography variant="body2" color="text.secondary">
              Nơi kết nối sinh viên và giảng viên, chia sẻ tài liệu và thông tin hoạt động khoa nhanh nhất.
            </Typography>
          </Grid>
          <Grid item xs={6} sm={2}>
            <Typography variant="subtitle2" fontWeight={700} gutterBottom>Khám phá</Typography>
            <Link href="/" color="inherit" display="block" sx={{ mb: 1, textDecoration: 'none' }}>Trang chủ</Link>
            <Link href="/profile" color="inherit" display="block" sx={{ mb: 1, textDecoration: 'none' }}>Cá nhân</Link>
          </Grid>
          <Grid item xs={12} sm={3}>
            <Typography variant="subtitle2" fontWeight={700} gutterBottom>Liên hệ</Typography>
            <Typography variant="body2" color="text.secondary">Văn phòng Khoa CNTT - Tòa nhà A1</Typography>
            <Typography variant="body2" color="text.secondary">Email: support.khoa@university.edu.vn</Typography>
          </Grid>
        </Grid>
        <Divider sx={{ my: 4 }} />
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap' }}>
          <Typography variant="body2" color="text.secondary">
            © {new Date().getFullYear()} Mini Social Network. Đồ án xây dựng mạng xã hội nội bộ.
          </Typography>
          <Box sx={{ display: 'flex', gap: 2 }}>
            <Typography variant="caption" color="text.disabled">Quyền riêng tư</Typography>
            <Typography variant="caption" color="text.disabled">Điều khoản</Typography>
          </Box>
        </Box>
      </Container>
    </Box>
  );
}