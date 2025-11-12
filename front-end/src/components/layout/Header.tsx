// src/components/layout/Header.tsx
import React from 'react';
import { 
  AppBar, Toolbar, Box, InputBase, 
  IconButton, Tooltip, Button, Avatar 
} from '@mui/material';
import { styled, alpha } from '@mui/material/styles';
import { Link as RouterLink } from 'react-router-dom';


// Import Icons
import SearchIcon from '@mui/icons-material/Search';
import HomeIcon from '@mui/icons-material/Home';

// ----------------- STYLED COMPONENTS (Giữ nguyên) -----------------
const Search = styled('div')(({ theme }) => ({
  position: 'relative',
  borderRadius: '20px', // Làm cho nó tròn như Facebook
  backgroundColor: alpha(theme.palette.common.white, 0.15),
  '&:hover': {
    backgroundColor: alpha(theme.palette.common.white, 0.25),
  },
  marginLeft: theme.spacing(2),
  width: '100%',
  [theme.breakpoints.up('sm')]: {
    marginLeft: theme.spacing(3),
    width: 'auto',
  },
}));

const SearchIconWrapper = styled('div')(({ theme }) => ({
  padding: theme.spacing(0, 2),
  height: '100%',
  position: 'absolute',
  pointerEvents: 'none',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
}));

const StyledInputBase = styled(InputBase)(({ theme }) => ({
  color: 'inherit',
  '& .MuiInputBase-input': {
    padding: theme.spacing(1, 1, 1, 0),
    // vertical padding + font size from searchIcon
    paddingLeft: `calc(1em + ${theme.spacing(4)})`,
    transition: theme.transitions.create('width'),
    width: '100%',
    [theme.breakpoints.up('md')]: {
      width: '20ch',
    },
  },
}));
// ----------------- END STYLED COMPONENTS -----------------


export default function Header() {
  const isLoggedIn = false;

  return (
    <AppBar position="sticky">
      {/* ⭐️ THAY ĐỔI: Thêm sx vào Toolbar để tạo bố cục 3 cột */}
      <Toolbar sx={{ justifyContent: 'space-between' }}>
        
        {/* ----- ⭐️ CỤM BÊN TRÁI ----- */}
        <Box sx={{ display: 'flex', alignItems: 'center' }}>
          {/* 1. Logo */}
          <Box
            component="img"
            src="/logo.png" // File này phải ở trong thư mục /public
            alt="Logo"
            sx={{ height: 40, cursor: 'pointer', mr: 2 }}
            onClick={() => (window.location.href = '/')} 
          />

          {/* 2. Khung Tìm kiếm */}
          <Search>
            <SearchIconWrapper>
              <SearchIcon />
            </SearchIconWrapper>
            <StyledInputBase
              placeholder="Tìm kiếm..."
              inputProps={{ 'aria-label': 'search' }}
            />
          </Search>
        </Box>


        {/* ----- ⭐️ CỤM Ở GIỮA (Nút Home) ----- */}
        {/* THAY ĐỔI: Bọc nút Home trong Box căn giữa & flexGrow */}
        <Box sx={{
          flexGrow: 1,
          display: 'flex',
          justifyContent: 'center',
        }}>
          {/* 3. Nút Home (Giữ nguyên style gốc của bạn) */}
          <Tooltip title="Trang chủ">
            <Box 
                component={RouterLink}
                to="/"
                sx={{ 
                    bgcolor: 'rgba(0,0,0,0.2)', // Nền nhẹ
                    borderRadius: '50%',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
            }}>
              <IconButton color="inherit">
                <HomeIcon sx={{ color: 'white' }} />
              </IconButton>
            </Box>
          </Tooltip>
        </Box>
        

        {/* ----- ⭐️ CỤM BÊN PHẢI ----- */}
        {/* THAY ĐỔI: Bọc cụm bên phải trong 1 Box */}
        <Box sx={{ display: 'flex', alignItems: 'center' }}>
          {isLoggedIn ? (
            // Nếu đã đăng nhập (xây dựng sau)
            <Tooltip title="Trang cá nhân">
              <IconButton sx={{ p: 0 }}>
                <Avatar alt="Tên Người Dùng" src="/static/images/avatar/1.jpg" />
              </IconButton>
            </Tooltip>
          ) : (
            // Nếu chưa đăng nhập
            <>
              <Button color="inherit" variant="outlined" sx={{ mr: 1 }}>
                Đăng nhập
              </Button>
              <Button color="inherit" variant="contained" sx={{ bgcolor: 'secondary.main' }}>
                Đăng ký
              </Button>
            </>
          )}
        </Box>

      </Toolbar>
    </AppBar>
  );
}