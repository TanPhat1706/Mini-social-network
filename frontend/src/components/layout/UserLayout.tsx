import { Box } from '@mui/material';
import Header from './Header';
import Footer from './Footer';
import React from 'react';

interface UserLayoutProps {
  children: React.ReactNode;
}

export const UserLayout: React.FC<UserLayoutProps> = ({ children }) => {
  return (
    <Box sx={{ 
      display: 'flex', 
      flexDirection: 'column', 
      minHeight: '100vh', 
      backgroundColor: '#F0F2F5' // Màu nền đặc trưng của Facebook
    }}>
      <Header />
      <main style={{ 
        flexGrow: 1, 
        paddingTop: '20px', 
        paddingBottom: '40px' 
      }}>
        {children}
      </main>
      <Footer />
    </Box>
  );
};