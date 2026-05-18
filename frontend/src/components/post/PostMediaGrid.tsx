import { useState } from 'react';
import { Box, Dialog, IconButton } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import { getApiBaseUrl } from '../../config/apiBase'; 

// 🟢 1. ĐỊNH NGHĨA KIỂU DỮ LIỆU CHO BỨC ẢNH VÀ PROPS
interface MediaItem {
  id?: number;
  url: string;
  type?: string;
}

interface PostMediaGridProps {
  media: MediaItem[];
}

interface SmartImageProps {
  src: string;
  alt: string;
  extraCount?: number; // Dấu "?" nghĩa là không bắt buộc phải có
}

// 🟢 2. GẮN TYPE VÀO COMPONENT CHÍNH
export default function PostMediaGrid({ media }: PostMediaGridProps) {
  const [open, setOpen] = useState(false);

  if (!media || media.length === 0) return null;

  const base = getApiBaseUrl();
  // 🟢 GẮN TYPE CHO HÀM getUrl
  const getUrl = (url: string) => (url.startsWith('http') ? url : `${base}${url}`);
  const handleOpen = () => setOpen(true);
  const handleClose = () => setOpen(false);

  // --- COMPONENT CON: Ô ẢNH THÔNG MINH ---
  // 🟢 3. GẮN TYPE CHO COMPONENT CON (Gán extraCount = 0 mặc định nếu không truyền)
  const SmartImage = ({ src, alt, extraCount = 0 }: SmartImageProps) => (
    <Box 
      sx={{ 
        width: '100%', 
        height: '100%', 
        position: 'relative', 
        overflow: 'hidden',
        backgroundColor: 'background.default' 
      }}
    >
      <img 
        src={src} 
        alt="" 
        style={{
          position: 'absolute',
          top: 0, left: 0,
          width: '100%', height: '100%',
          objectFit: 'cover',   
          filter: 'blur(20px)', 
          transform: 'scale(1.1)', 
          opacity: 0.8 
        }} 
      />

      <img 
        src={src} 
        alt={alt} 
        style={{
          position: 'relative',
          width: '100%', height: '100%',
          objectFit: 'contain', 
          zIndex: 2 
        }} 
      />

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

  const imageContainerStyle = {
    width: '100%',
    height: '100%',
    position: 'relative',
    cursor: 'pointer',
    overflow: 'hidden',
  };

  const maxDisplay = 4;

  const renderGridLayout = () => {
    const count = media.length;

    if (count === 1) {
      return (
        <Box onClick={handleOpen} sx={{ ...imageContainerStyle, height: 400 }}>
          {/* 🟢 KHÔNG CẦN TRUYỀN extraCount NỮA VÌ ĐÃ CÓ DẤU ? Ở TYPE */}
          <SmartImage src={getUrl(media[0].url)} alt="single" />
        </Box>
      );
    }

    if (count === 2) {
      return (
        <Box onClick={handleOpen} sx={{ display: 'flex', gap: 0.5, height: 300, width: '100%' }}> 
          {/* 🟢 4. GẮN TYPE CHO BIẾN MAP */}
          {media.map((item: MediaItem, idx: number) => (
             <Box 
                key={idx} 
                sx={{ 
                    flex: 1, 
                    ...imageContainerStyle, 
                    minWidth: 0 
                }}
             > 
                <SmartImage src={getUrl(item.url)} alt={`img-${idx}`} />
             </Box>
          ))}
        </Box>
      );
    }

    if (count === 3) {
      return (
        <Box onClick={handleOpen} sx={{ display: 'flex', gap: 0.5, height: 350, width: '100%' }}>
          <Box sx={{ flex: 2, ...imageContainerStyle, minWidth: 0 }}>
             <SmartImage src={getUrl(media[0].url)} alt="img-0" />
          </Box>
          <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 0.5, minWidth: 0 }}>
            <Box sx={{ flex: 1, ...imageContainerStyle, minHeight: 0 }}>
                <SmartImage src={getUrl(media[1].url)} alt="img-1" />
            </Box>
            <Box sx={{ flex: 1, ...imageContainerStyle, minHeight: 0 }}>
                <SmartImage src={getUrl(media[2].url)} alt="img-2" />
            </Box>
          </Box>
        </Box>
      );
    }

    if (count >= 4) {
        const extraCount = count - 4;
        return (
          <Box sx={{ display: 'flex', gap: 0.5, height: 350, width: '100%' }}>
            <Box sx={{ flex: 2, ...imageContainerStyle, minWidth: 0 }} onClick={handleOpen}>
              <SmartImage src={getUrl(media[0].url)} alt="img-0" />
            </Box>
    
            <Box sx={{ flex: 1, display: 'grid', gridTemplateRows: '1fr 1fr 1fr', gap: 0.5, minWidth: 0 }}>
              {media.slice(1, 4).map((item: MediaItem, idx: number) => {
                const isLast = idx === 2 && extraCount > 0;
                return (
                  <Box key={idx} sx={{ ...imageContainerStyle, position: 'relative' }} onClick={handleOpen}>
                    <SmartImage 
                        src={getUrl(item.url)} 
                        alt={`img-${idx+1}`} 
                        extraCount={isLast ? extraCount : 0} 
                    />
                  </Box>
                );
              })}
            </Box>
          </Box>
        );
      }
  };

  return (
    <Box sx={{ mt: 1, mb: 1, width: '100%', maxWidth: '100%', overflow: 'hidden' }}>
        {renderGridLayout()}
        <Dialog 
            open={open} 
            onClose={handleClose} 
            fullWidth maxWidth="md" scroll="body"
        >
            <Box sx={{ position: 'relative', bgcolor: 'black', minHeight: '500px', p: 0 }}>
                <IconButton onClick={handleClose} sx={{ position: 'absolute', top: 10, right: 10, color: 'white', zIndex: 999, bgcolor: 'rgba(0,0,0,0.3)' }}><CloseIcon /></IconButton>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, p: 2, alignItems: 'center' }}>
                    {/* 🟢 GẮN THÊM TYPE Ở ĐÂY NỮA */}
                    {media.map((item: MediaItem) => (
                        <img key={item.id || item.url} src={getUrl(item.url)} alt="detail" style={{ maxWidth: '100%', maxHeight: '80vh', borderRadius: 4 }} />
                    ))}
                </Box>
            </Box>
        </Dialog>
    </Box>
  );
}