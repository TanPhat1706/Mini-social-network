import React from 'react';
import { Box, Container, Typography, Link, Divider } from '@mui/material';

export default function Footer() {
  return (
    <Box
      component="footer"
      sx={{
        py: 4,
        px: 2,
        mt: 'auto',
        backgroundColor: '#FFFFFF',
        borderTop: '1px solid #E4E6E9'
      }}
    >
      <Container maxWidth="lg">
        <Typography variant="body2" color="#65676B" align="center" sx={{ fontWeight: 500 }}>
          Mini Social Network - Nền tảng kết nối sinh viên hiện đại
        </Typography>
        <Typography variant="caption" color="text.secondary" align="center" display="block" sx={{ mt: 1 }}>
          {'Bản quyền © '}
          <Link color="inherit" href="/" sx={{ textDecoration: 'none', fontWeight: 'bold' }}>
            MiniSocial
          </Link>{' '}
          {new Date().getFullYear()}
          {'. Toàn bộ quyền được bảo lưu.'}
        </Typography>
      </Container>
    </Box>
  );
}