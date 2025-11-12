import { Box } from '@mui/material';
import Header from './components/layout/Header';
import Footer from './components/layout/Footer';

// 1. Import Routes và Route
import { Routes, Route } from 'react-router-dom';

// 2. Import các trang
import HomePage from './pages/HomePage';
import ProfilePage from './pages/ProfilePage';

function App() {
  return (
    <Box 
      sx={{ 
        display: 'flex', 
        flexDirection: 'column', 
        minHeight: '100vh',
        bgcolor: 'background.default'
      }}
    >
      {/* Header và Footer sẽ hiển thị trên mọi trang */}
      <Header />

      {/* 3. Phần nội dung chính (thay đổi theo URL) */}
      <main style={{ flexGrow: 1 }}>
        <Routes>
          {/* Route cho Trang chủ */}
          <Route path="/" element={<HomePage />} />
          
          {/* Route cho Trang cá nhân (ví dụ: /profile/lehuynhphat) */}
          <Route path="/profile/:userId" element={<ProfilePage />} />
          
          {/* Route dự phòng (ví dụ: /profile trỏ về trang của mình) */}
          {/* Tạm thời chúng ta cũng cho nó trỏ về ProfilePage */}
          <Route path="/profile" element={<ProfilePage />} />
          
          {/* TODO: Thêm các route khác (Login, Register...) */}
        </Routes>
      </main>
      
      <Footer />
    </Box>
  );
}

export default App;