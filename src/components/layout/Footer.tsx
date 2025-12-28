// src/components/layout/Footer.tsx
import React from 'react';
import { Box, Container, Typography, Link } from '@mui/material';

export default function Footer() {
  return (
    <Box
      component="footer"
      sx={{
        py: 3,
        px: 2,
        mt: 'auto', // Đẩy footer xuống cuối trang
        backgroundColor: (theme) =>
          theme.palette.mode === 'light'
            ? theme.palette.grey[200]
            : theme.palette.grey[800],
      }}
    >
      <Container maxWidth="lg">
        <Typography variant="body1" align="center">
          Mini Social Network - Đồ án của bạn
        </Typography>
        <Typography
          variant="body2"
          color="text.secondary"
          align="center"
        >
          {'Bản quyền © '}
          <Link color="inherit" href="#">
            Tên của bạn
          </Link>{' '}
          {new Date().getFullYear()}
          {'.'}
        </Typography>
      </Container>
    </Box>
  );
}