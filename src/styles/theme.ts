// src/styles/theme.ts
import { createTheme } from '@mui/material/styles';

// -- BẮT ĐẦU VÙNG TÙY CHỈNH CỦA BẠN --

// 1. ĐỊNH NGHĨA MÀU CHÍNH
// Hãy thay đổi các mã màu HEX này theo ý bạn.
// Bạn có thể dùng các công cụ như 'coolors.co' để chọn
const PRIMARY_COLOR = '#1976D2';   // Xanh dương (Màu hiện tại của Header)
const SECONDARY_COLOR = '#ED6C02'; // Cam (Màu hiện tại của nút Đăng ký)
const SUCCESS_COLOR = '#2e7d32';   // Xanh lá
const ERROR_COLOR = '#d32f2f';     // Đỏ
const WARNING_COLOR = '#ed6c02';   // Vàng/Cam
const INFO_COLOR = '#0288d1';      // Xanh thông tin

// 2. ĐỊNH NGHĨA MÀU NỀN
// Rất quan trọng cho một giao diện "sạch"
const BACKGROUND_DEFAULT = '#f4f6f8'; // Màu nền xám rất nhạt (cho body)
const BACKGROUND_PAPER = '#FFFFFF';   // Màu nền trắng tinh (cho Card, Paper...)

// 3. ĐỊNH NGHĨA FONT VÀ CỠ CHỮ
const FONT_FAMILY = 'Roboto, sans-serif';
const FONT_SIZE_DEFAULT = 14; // Cỡ chữ mặc định (14px)

// 4. ĐỊNH NGHĨA ĐỘ BO GÓC
const BORDER_RADIUS_DEFAULT = 8; // Bo góc 8px

// -- KẾT THÚC VÙNG TÙY CHỈNH --


// ----------------------------------------------------------------------
// 🚀 TẠO THEME
// ----------------------------------------------------------------------

const theme = createTheme({
  // 🎨 I. BẢNG MÀU
  palette: {
    mode: 'light',
    primary: {
      main: PRIMARY_COLOR,
    },
    secondary: {
      main: SECONDARY_COLOR,
    },
    success: {
      main: SUCCESS_COLOR,
    },
    error: {
      main: ERROR_COLOR,
    },
    warning: {
      main: WARNING_COLOR,
    },
    info: {
      main: INFO_COLOR,
    },
    background: {
      default: BACKGROUND_DEFAULT,
      paper: BACKGROUND_PAPER,
    },
    text: {
      primary: '#212121',   // Màu chữ đen (nhưng không quá gắt)
      secondary: '#616161', // Màu chữ xám (cho text phụ, timestamp)
      disabled: '#BDBDBD',  // Màu chữ bị vô hiệu hóa
    },
  },

  // 🖋️ II. KIỂU CHỮ
  typography: {
    fontFamily: FONT_FAMILY,
    htmlFontSize: 16, // Giữ 16px làm gốc (tốt cho accessibility)
    fontSize: FONT_SIZE_DEFAULT,
    h1: { fontSize: '2.5rem', fontWeight: 700 },
    h2: { fontSize: '2rem', fontWeight: 600 },
    h3: { fontSize: '1.75rem', fontWeight: 600 },
    h4: { fontSize: '1.5rem', fontWeight: 500 },
    h5: { fontSize: '1.25rem', fontWeight: 500 },
    h6: { fontSize: '1rem', fontWeight: 500 },
    body1: { fontSize: '1rem' }, // 16px
    body2: { fontSize: '0.875rem' }, // 14px (cỡ chữ mặc định của chúng ta)
    button: {
      fontWeight: 600, // Làm cho chữ trên nút bấm đậm hơn
      textTransform: 'none', // ⭐️ Bỏ VIẾT HOA chữ trên nút
    },
  },

  // 📐 III. HÌNH DẠNG & GIÃN CÁCH
  shape: {
    borderRadius: BORDER_RADIUS_DEFAULT,
  },
  
  // (Chúng ta giữ nguyên `spacing` 8px mặc định của MUI)
  // theme.spacing(1) = 8px
  // theme.spacing(2) = 16px

  // ✨ IV. GHI ĐÈ STYLE MẶC ĐỊNH
  // Đây là nơi "tỉ mỉ" phát huy tác dụng
  components: {
    // Tùy chỉnh cho tất cả <Button>
    MuiButton: {
      styleOverrides: {
        root: {
          // Áp dụng bóng (shadow) nhẹ cho nút "contained"
          boxShadow: '0px 2px 4px rgba(0, 0, 0, 0.08)',
          '&:hover': {
            boxShadow: '0px 4px 10px rgba(0, 0, 0, 0.12)',
          }
        },
        containedPrimary: {
          // Style riêng cho nút <Button variant="contained" color="primary">
          color: 'white', // Đảm bảo chữ luôn trắng
        },
      },
    },

    // Tùy chỉnh cho tất cả <Card>
    MuiCard: {
      styleOverrides: {
        root: {
          // Bỏ box-shadow mặc định của Card, thay bằng viền (border)
          // Tạo cảm giác "sạch" và "phẳng" hơn
          border: `1px solid #E0E0E0`,
          boxShadow: 'none', 
          // Nếu bạn vẫn thích shadow, hãy dùng:
          // boxShadow: '0px 4px 12px rgba(0, 0, 0, 0.05)',
        },
      },
    },

    // Tùy chỉnh cho tất cả <Input> (TextField)
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          // Style cho ô input khi không được focus
          backgroundColor: BACKGROUND_PAPER, // Nền trắng
          '& .MuiOutlinedInput-notchedOutline': {
            borderColor: '#E0E0E0',
          },
          '&:hover .MuiOutlinedInput-notchedOutline': {
            borderColor: PRIMARY_COLOR,
          },
          // Style khi đang focus
          '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
            borderWidth: '1px',
          },
        },
      },
    },
    
    // Tùy chỉnh cho <AppBar> (Header)
    MuiAppBar: {
      styleOverrides: {
        root: {
          // Bỏ shadow của AppBar, thay bằng đường viền dưới
          boxShadow: 'none',
          borderBottom: `1px solid ${alpha(PRIMARY_COLOR, 0.3)}`,
        }
      }
    }
  },
});

// Phải import 'alpha' nếu bạn dùng nó trong 'components'
import { alpha } from '@mui/material/styles';

export default theme;