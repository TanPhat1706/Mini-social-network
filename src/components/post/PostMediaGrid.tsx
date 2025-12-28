import { useState } from 'react';
import { Box, Dialog, IconButton } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';

// Định nghĩa URL cơ sở 
const BASE_URL = 'https://mini-social-network-ayab.onrender.com'; 

export default function PostMediaGrid({ media }: { media: any[] }) {
  const [open, setOpen] = useState(false);

  if (!media || media.length === 0) return null;

  const getUrl = (url) => (url.startsWith('http') ? url : `${BASE_URL}${url}`);
  const handleOpen = () => setOpen(true);
  const handleClose = () => setOpen(false);

  // --- COMPONENT CON: Ô ẢNH THÔNG MINH ---
  // Tách ra để tái sử dụng logic Blurred Background
  const SmartImage = ({ src, alt, extraCount }) => (
    <Box 
      sx={{ 
        width: '100%', 
        height: '100%', 
        position: 'relative', 
        overflow: 'hidden',
        backgroundColor: '#f0f2f5' // Màu nền dự phòng nhẹ nhàng
      }}
    >
      {/* LỚP 1: NỀN MỜ (Luôn cover hết khung) */}
      <img 
        src={src} 
        alt="" 
        style={{
          position: 'absolute',
          top: 0, left: 0,
          width: '100%', height: '100%',
          objectFit: 'cover',   // Lấp đầy khung
          filter: 'blur(20px)', // Làm mờ
          transform: 'scale(1.1)', // Phóng to nhẹ để che viền mờ
          opacity: 0.8 // Làm nhạt bớt cho đỡ gắt
        }} 
      />

      {/* LỚP 2: ẢNH CHÍNH (Hiển thị đầy đủ nội dung) */}
      <img 
        src={src} 
        alt={alt} 
        style={{
          position: 'relative',
          width: '100%', height: '100%',
          objectFit: 'contain', // Quan trọng: Giữ nguyên tỉ lệ ảnh gốc
          zIndex: 2 // Nổi lên trên
        }} 
      />

      {/* LỚP 3: Overlay số lượng (nếu có) */}
      {extraCount > 0 && (
        <Box
          sx={{
            position: 'absolute',
            inset: 0,
            backgroundColor: 'rgba(0,0,0,0.5)',
            color: '#fff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: 32,
            fontWeight: 600,
            zIndex: 3
          }}
        >
          +{extraCount}
        </Box>
      )}
    </Box>
  );

  // CSS chung cho tất cả các ô chứa ảnh
  const imageContainerStyle = {
    width: '100%',
    height: '100%',
    position: 'relative',
    cursor: 'pointer',
    overflow: 'hidden',
    
    // // ⭐️ THÊM MỚI:
    // backgroundColor: '#000', // Nền đen để lấp vào khoảng trống
    // display: 'flex',         // Dùng Flexbox để căn giữa ảnh trong khoảng đen
    // alignItems: 'center',    // Căn giữa dọc
    // justifyContent: 'center' // Căn giữa ngang
  };

  const imgStyle = {
    width: '100%',      
    height: '100%',     
    
    // ⭐️ THAY ĐỔI QUAN TRỌNG:
    objectFit: 'contain', // Co giãn ảnh để hiển thị TOÀN BỘ nội dung, không cắt
    
    display: 'block',
  };

  const maxDisplay = 4;
  const remainingCount = media.length - maxDisplay;

  // --- LOGIC RENDER GRID --- //

  const renderGridLayout = () => {
    const count = media.length;

    // TRƯỜNG HỢP 1 ẢNH: (Không đổi)
    if (count === 1) {
      return (
        <Box onClick={handleOpen} sx={{ ...imageContainerStyle, height: 400 }}>
          <SmartImage src={getUrl(media[0].url)} alt="single" extraCount={undefined} />
        </Box>
      );
    }

    // ⭐️ TRƯỜNG HỢP 2 ẢNH: Fix Overflow
    if (count === 2) {
      return (
        <Box onClick={handleOpen} sx={{ display: 'flex', gap: 0.5, height: 300, width: '100%' }}> 
          {media.map((item, idx) => (
             <Box 
                key={idx} 
                sx={{ 
                    flex: 1, 
                    ...imageContainerStyle, 
                    minWidth: 0 // ⭐️ QUAN TRỌNG: Cho phép co nhỏ dưới kích thước content
                }}
             > 
                <SmartImage src={getUrl(item.url)} alt={`img-${idx}`} extraCount={undefined} />
             </Box>
          ))}
        </Box>
      );
    }

    // ⭐️ TRƯỜNG HỢP 3 ẢNH: Fix Overflow (Đây là chỗ bạn đang thiếu minWidth)
    if (count === 3) {
      return (
        <Box onClick={handleOpen} sx={{ display: 'flex', gap: 0.5, height: 350, width: '100%' }}>
          {/* Cột trái: Ảnh to */}
          <Box 
            sx={{ 
                flex: 2, 
                ...imageContainerStyle, 
                minWidth: 0 // ⭐️ QUAN TRỌNG
            }}
          >
             <SmartImage src={getUrl(media[0].url)} alt="img-0" extraCount={undefined} />
          </Box>
          
          {/* Cột phải: 2 ảnh nhỏ */}
          <Box 
            sx={{ 
                flex: 1, 
                display: 'flex', 
                flexDirection: 'column', 
                gap: 0.5,
                minWidth: 0 // ⭐️ QUAN TRỌNG: Cần thêm cả ở container cột phải
            }}
          >
            <Box sx={{ flex: 1, ...imageContainerStyle, minHeight: 0 }}>
                <SmartImage src={getUrl(media[1].url)} alt="img-1" extraCount={undefined} />
            </Box>
            <Box sx={{ flex: 1, ...imageContainerStyle, minHeight: 0 }}>
                <SmartImage src={getUrl(media[2].url)} alt="img-2" extraCount={undefined} />
            </Box>
          </Box>
        </Box>
      );
    }

    // 4+ ẢNH (Grid CSS thường ít bị lỗi này hơn Flex, nhưng nên thêm minWidth cho an toàn)
    if (count >= 4) {
        const extraCount = count - 4;
        return (
          <Box sx={{ display: 'flex', gap: 0.5, height: 350, width: '100%' }}>
            <Box sx={{ flex: 2, ...imageContainerStyle, minWidth: 0 }} onClick={handleOpen}>
              <SmartImage src={getUrl(media[0].url)} alt="img-0" extraCount={undefined} />
            </Box>
    
            <Box sx={{ flex: 1, display: 'grid', gridTemplateRows: '1fr 1fr 1fr', gap: 0.5, minWidth: 0 }}>
              {media.slice(1, 4).map((item, idx) => {
                const isLast = idx === 2 && extraCount > 0;
                return (
                  <Box key={idx} sx={{ ...imageContainerStyle, position: 'relative' }} onClick={handleOpen}>
                    <SmartImage 
                        src={getUrl(item.url)} 
                        alt={`img-${idx+1}`} 
                        extraCount={isLast ? extraCount : 0} 
                    />
                    {isLast && (
                      <Box sx={{ position: 'absolute', inset: 0, bgcolor: 'rgba(0,0,0,0.5)', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 32, fontWeight: 600 }}>
                        +{extraCount}
                      </Box>
                    )}
                  </Box>
                );
              })}
            </Box>
          </Box>
        );
      }
  };

  return (
    // Wrapper ngoài cùng giữ nguyên
    <Box sx={{ mt: 1, mb: 1, width: '100%', maxWidth: '100%', overflow: 'hidden' }}>
        {renderGridLayout()}
        {/* ... Dialog code ... */}
        <Dialog 
            open={open} 
            onClose={handleClose} 
            fullWidth maxWidth="md" scroll="body"
        >
            <Box sx={{ position: 'relative', bgcolor: 'black', minHeight: '500px', p: 0 }}>
                <IconButton onClick={handleClose} sx={{ position: 'absolute', top: 10, right: 10, color: 'white', zIndex: 999, bgcolor: 'rgba(0,0,0,0.3)' }}><CloseIcon /></IconButton>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, p: 2, alignItems: 'center' }}>
                    {media.map((item) => (
                        <img key={item.id} src={getUrl(item.url)} alt="detail" style={{ maxWidth: '100%', maxHeight: '80vh', borderRadius: 4 }} />
                    ))}
                </Box>
            </Box>
        </Dialog>
    </Box>
  );
}