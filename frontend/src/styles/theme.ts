// src/styles/theme.ts
import { createContext, useMemo, useState, useContext, useEffect } from 'react';
import { createTheme,type Theme, alpha } from '@mui/material/styles';

// ----------------------------------------------------------------------
// 1. CONTEXT: Cái công tắc để các Component khác bấm vào
// ----------------------------------------------------------------------
export const ColorModeContext = createContext({
  toggleColorMode: () => {},
  mode: 'light' as 'light' | 'dark'
});

export const useColorMode = () => useContext(ColorModeContext);

// ----------------------------------------------------------------------
// 2. BIẾN MÀU CỐ ĐỊNH (Dùng chung cho cả Sáng/Tối)
// ----------------------------------------------------------------------
const PRIMARY_COLOR = '#1877F2';
const SECONDARY_COLOR = '#42b72a';
const SUCCESS_COLOR = '#2e7d32';
const ERROR_COLOR = '#d32f2f';
const WARNING_COLOR = '#ed6c02';
const INFO_COLOR = '#0288d1';
const FONT_FAMILY = 'Segoe UI, Helvetica, Arial, sans-serif';
const BORDER_RADIUS_DEFAULT = 8;

// ----------------------------------------------------------------------
// 3. HOOK TẠO THEME: Trái tim của hệ thống
// ----------------------------------------------------------------------
export const useThemeConfig = () => {
  // Lấy trạng thái lưu trong localStorage, nếu không có thì mặc định là 'light'
  const [mode, setMode] = useState<'light' | 'dark'>(() => {
    const savedMode = localStorage.getItem('themeMode');
    return (savedMode === 'dark') ? 'dark' : 'light';
  });

  const toggleColorMode = () => {
    setMode((prevMode) => {
      const newMode = prevMode === 'light' ? 'dark' : 'light';
      localStorage.setItem('themeMode', newMode); // Nhớ trạng thái cho lần sau
      return newMode;
    });
  };

  const theme: Theme = useMemo(
    () =>
      createTheme({
        palette: {
          mode, // 🟢 ĐIỂM SÁNG: Gắn biến mode vào đây để MUI tự tính toán lại màu
          primary: { main: PRIMARY_COLOR },
          secondary: { main: SECONDARY_COLOR },
          success: { main: SUCCESS_COLOR },
          error: { main: ERROR_COLOR },
          warning: { main: WARNING_COLOR },
          info: { main: INFO_COLOR },
          
          // 🟢 CẤU HÌNH MÀU THEO CHẾ ĐỘ (Toán tử 3 ngôi: Sáng ? Tối)
          background: {
            default: mode === 'light' ? '#F0F2F5' : '#18191A', // Xám nhạt FB hoặc Đen mun FB
            paper: mode === 'light' ? '#FFFFFF' : '#242526',   // Trắng tinh hoặc Xám đen
          },
          text: {
            primary: mode === 'light' ? '#050505' : '#E4E6EB', // Chữ đen hoặc Chữ trắng xám
            secondary: mode === 'light' ? '#65676B' : '#B0B3B8',
          },
        },

        // --- CÁC PHẦN DƯỚI NÀY GIỮ NGUYÊN 100% STYLE CHUẨN CỦA BẠN ---
        typography: {
          fontFamily: FONT_FAMILY,
          button: { fontWeight: 600, textTransform: 'none' },
        },
        shape: { borderRadius: BORDER_RADIUS_DEFAULT },
        components: {
          MuiButton: {
            styleOverrides: {
              root: {
                borderRadius: 6,
                padding: '8px 16px',
                boxShadow: 'none',
              },
            },
          },
          MuiCard: {
            styleOverrides: {
              root: {
                border: 'none',
                boxShadow: '0 1px 2px rgba(0, 0, 0, 0.1)', // Có thể tinh chỉnh shadow dark ở đây sau nếu thích
                borderRadius: BORDER_RADIUS_DEFAULT,
              },
            },
          },
          MuiOutlinedInput: {
            styleOverrides: {
              root: {
                backgroundColor: mode === 'light' ? '#F0F2F5' : '#3A3B3C', // 🟢 Input đổi màu theo mode
                borderRadius: 20,
              },
            },
          },
          MuiAppBar: {
            styleOverrides: {
              root: {
                backgroundColor: mode === 'light' ? '#FFFFFF' : '#242526', // 🟢 Header đổi màu theo mode
                color: mode === 'light' ? '#050505' : '#E4E6EB',
                boxShadow: '0 1px 2px rgba(0, 0, 0, 0.1)',
                borderBottom: `1px solid ${mode === 'light' ? alpha('#000', 0.1) : alpha('#FFF', 0.1)}`,
              }
            }
          }
        },
      }),
    [mode]
  );

  return { theme, toggleColorMode, mode };
};