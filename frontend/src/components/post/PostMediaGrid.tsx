import { useState } from 'react';
import { Box, Dialog, IconButton } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import { getApiBaseUrl } from '../../config/apiBase';
import PlayCircleOutlineIcon from '@mui/icons-material/PlayCircleOutline';

interface MediaItem {
  id?: number;
  url: string;
  type?: string;
}

interface PostMediaGridProps {
  media: MediaItem[];
}

interface SmartMediaProps {
  item: MediaItem;
  alt: string;
  extraCount?: number;
}

export default function PostMediaGrid({ media }: PostMediaGridProps) {
  const [open, setOpen] = useState(false);

  if (!media || media.length === 0) return null;

  const base = getApiBaseUrl();
  const getUrl = (url: string) => (url.startsWith('http') ? url : `${base}${url}`);
  const handleOpen = () => setOpen(true);
  const handleClose = () => setOpen(false);

  const isVideo = (item: MediaItem) => {
    if (item.type && item.type.startsWith('video/')) return true;
    const urlLower = item.url.toLowerCase();
    return urlLower.endsWith('.mp4') || urlLower.endsWith('.webm') || urlLower.endsWith('.mov');
  }

  const SmartMedia = ({ item, alt, extraCount = 0 }: SmartMediaProps) => {
    const src = getUrl(item.url);
    const isVid = isVideo(item);

    return (
      <Box
        sx={{
          width: '100%',
          height: '100%',
          position: 'relative',
          overflow: 'hidden',
          backgroundColor: 'black'
        }}
      >
        {isVid ? (
          <>
            <video
              src={src}
              muted autoPlay loop playsInline
              style={{
                position: 'absolute', top: 0, left: 0,
                width: '100%', height: '100%',
                objectFit: 'cover', filter: 'blur(10px)', transform: 'scale(1.1)', opacity: 0.5
              }}
            />
            <Box
              sx={{
                position: 'absolute', top: '50%', left: '50%',
                transform: 'translate(-50%, -50%)', zIndex: 3,
                color: 'rgba(255,255,255,0.8)'
              }}
            >
              <PlayCircleOutlineIcon sx={{ fontSize: 48 }} />
            </Box>
          </>
        ) : (
          <>
            <img
              src={src} alt=""
              style={{
                position: 'absolute', top: 0, left: 0,
                width: '100%', height: '100%',
                objectFit: 'cover', filter: 'blur(20px)', transform: 'scale(1.1)', opacity: 0.8
              }}
            />
            <img
              src={src} alt={alt}
              style={{
                position: 'relative', width: '100%', height: '100%',
                objectFit: 'contain', zIndex: 2
              }}
            />
          </>
        )}

        {extraCount > 0 && (
          <Box
            sx={{
              position: 'absolute', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)',
              color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 32, fontWeight: 600, zIndex: 4
            }}
          >
            +{extraCount}
          </Box>
        )}
      </Box>
    );
  };

  const imageContainerStyle = {
    width: '100%', height: '100%', position: 'relative', cursor: 'pointer', overflow: 'hidden',
  };

  const renderGridLayout = () => {
    const count = media.length;

    if (count === 1) {
      return (
        <Box onClick={handleOpen} sx={{ ...imageContainerStyle, height: 400 }}>
          <SmartMedia item={media[0]} alt="single" />
        </Box>
      );
    }

    if (count === 2) {
      return (
        <Box onClick={handleOpen} sx={{ display: 'flex', gap: 0.5, height: 300, width: '100%' }}>
          {media.map((item, idx) => (
            <Box key={idx} sx={{ flex: 1, ...imageContainerStyle, minWidth: 0 }}>
              <SmartMedia item={item} alt={`img-${idx}`} />
            </Box>
          ))}
        </Box>
      );
    }

    if (count === 3) {
      return (
        <Box onClick={handleOpen} sx={{ display: 'flex', gap: 0.5, height: 350, width: '100%' }}>
          <Box sx={{ flex: 2, ...imageContainerStyle, minWidth: 0 }}>
            <SmartMedia item={media[0]} alt="img-0" />
          </Box>
          <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 0.5, minWidth: 0 }}>
            <Box sx={{ flex: 1, ...imageContainerStyle, minHeight: 0 }}>
              <SmartMedia item={media[1]} alt="img-1" />
            </Box>
            <Box sx={{ flex: 1, ...imageContainerStyle, minHeight: 0 }}>
              <SmartMedia item={media[2]} alt="img-2" />
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
            <SmartMedia item={media[0]} alt="img-0" />
          </Box>
          <Box sx={{ flex: 1, display: 'grid', gridTemplateRows: '1fr 1fr 1fr', gap: 0.5, minWidth: 0 }}>
            {media.slice(1, 4).map((item, idx) => {
              const isLast = idx === 2 && extraCount > 0;
              return (
                <Box key={idx} sx={{ ...imageContainerStyle, position: 'relative' }} onClick={handleOpen}>
                  <SmartMedia item={item} alt={`img-${idx + 1}`} extraCount={isLast ? extraCount : 0} />
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
        open={open} onClose={handleClose}
        fullWidth maxWidth="md" scroll="body"
      >
        <Box sx={{ position: 'relative', bgcolor: 'black', minHeight: '500px', p: 0 }}>
          <IconButton onClick={handleClose} sx={{ position: 'absolute', top: 10, right: 10, color: 'white', zIndex: 999, bgcolor: 'rgba(0,0,0,0.3)' }}><CloseIcon /></IconButton>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, p: 2, alignItems: 'center' }}>
            {media.map((item) => (
              isVideo(item) ? (
                <video
                  key={item.id || item.url}
                  src={getUrl(item.url)}
                  controls
                  autoPlay
                  style={{ maxWidth: '100%', maxHeight: '80vh', borderRadius: 4 }}
                />
              ) : (
                <img
                  key={item.id || item.url}
                  src={getUrl(item.url)}
                  alt="detail"
                  style={{ maxWidth: '100%', maxHeight: '80vh', borderRadius: 4 }}
                />
              )
            ))}
          </Box>
        </Box>
      </Dialog>
    </Box>
  );
}