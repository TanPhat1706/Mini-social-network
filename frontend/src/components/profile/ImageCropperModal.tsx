import React, { useState, useRef, useEffect } from 'react';
import { Dialog, DialogTitle, DialogContent, DialogActions, Button, Slider, Box, IconButton, Typography } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import ZoomInIcon from '@mui/icons-material/ZoomIn';
import ZoomOutIcon from '@mui/icons-material/ZoomOut';

interface Props {
  open: boolean;
  imageSrc: string | null;
  aspectRatio: number; // 1 cho Avatar, 16/9 cho Cover
  onClose: () => void;
  onCropDone: (croppedFile: File) => void;
}

export default function ImageCropperModal({ open, imageSrc, aspectRatio, onClose, onCropDone }: Props) {
  const [zoom, setZoom] = useState(1);
  const [position, setPosition] = useState({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);
  
  // 🟢 STATE MỚI: Lưu kích thước thật của ảnh sau khi tính toán để tránh bị CSS xén mất
  const [imgSize, setImgSize] = useState({ width: 0, height: 0 });
  
  const dragStart = useRef({ x: 0, y: 0 });
  const imageRef = useRef<HTMLImageElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  // Reset state mỗi khi mở ảnh mới
  useEffect(() => {
    if (open) {
      setZoom(1);
      setPosition({ x: 0, y: 0 });
      setImgSize({ width: 0, height: 0 }); // Reset kích thước
    }
  }, [open, imageSrc]);

  // 🟢 HÀM MỚI: Tự động tính toán kích thước ảnh ngay khi load xong
  const handleImageLoad = (e: React.SyntheticEvent<HTMLImageElement>) => {
    const { naturalWidth, naturalHeight } = e.currentTarget;
    if (!containerRef.current) return;
    
    // Lấy kích thước khung chứa
    const { width: contW, height: contH } = containerRef.current.getBoundingClientRect();

    // Tính tỷ lệ phóng to tối thiểu để ảnh lấp đầy khung
    const scale = Math.max(contW / naturalWidth, contH / naturalHeight);
    
    // Áp dụng kích thước để ảnh render tràn viền, giữ lại toàn bộ pixel gốc
    setImgSize({
      width: naturalWidth * scale,
      height: naturalHeight * scale
    });
  };

  const handleMouseDown = (e: React.MouseEvent | React.TouchEvent) => {
    setIsDragging(true);
    const clientX = 'touches' in e ? e.touches[0].clientX : (e as React.MouseEvent).clientX;
    const clientY = 'touches' in e ? e.touches[0].clientY : (e as React.MouseEvent).clientY;
    dragStart.current = { x: clientX - position.x, y: clientY - position.y };
  };

  const handleMouseMove = (e: React.MouseEvent | React.TouchEvent) => {
    if (!isDragging) return;
    const clientX = 'touches' in e ? e.touches[0].clientX : (e as React.MouseEvent).clientX;
    const clientY = 'touches' in e ? e.touches[0].clientY : (e as React.MouseEvent).clientY;
    setPosition({
      x: clientX - dragStart.current.x,
      y: clientY - dragStart.current.y
    });
  };

  const handleMouseUp = () => { setIsDragging(false); };

  const handleSave = async () => {
    if (!imageRef.current || !containerRef.current) return;
    
    const image = imageRef.current;
    const container = containerRef.current;
    
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const containerRect = container.getBoundingClientRect();
    const contW = containerRect.width;
    const contH = containerRect.height;
    const natW = image.naturalWidth;
    const natH = image.naturalHeight;

    const baseScale = Math.max(contW / natW, contH / natH);

    const sx = (natW / 2) - ((contW / 2) + position.x) / (baseScale * zoom);
    const sy = (natH / 2) - ((contH / 2) + position.y) / (baseScale * zoom);
    const sWidth = contW / (baseScale * zoom);
    const sHeight = contH / (baseScale * zoom);

    canvas.width = contW * 2; 
    canvas.height = contH * 2;
    ctx.scale(2, 2);

    ctx.fillStyle = "#000000";
    ctx.fillRect(0, 0, contW, contH);

    ctx.drawImage(
      image,
      sx, sy, sWidth, sHeight,  
      0, 0, contW, contH        
    );

    canvas.toBlob((blob) => {
      if (blob) {
        const file = new File([blob], "cropped_image.jpg", { type: "image/jpeg", lastModified: Date.now() });
        onCropDone(file);
      }
    }, 'image/jpeg', 0.9); 
  };

  if (!imageSrc) return null;

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h6" fontWeight="bold">Điều chỉnh ảnh</Typography>
        <IconButton onClick={onClose}><CloseIcon /></IconButton>
      </DialogTitle>
      
      <DialogContent dividers sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <Box 
          ref={containerRef}
          sx={{
            width: '100%',
            maxWidth: 400,
            aspectRatio: `${aspectRatio}`, 
            overflow: 'hidden',
            position: 'relative',
            bgcolor: '#000',
            borderRadius: aspectRatio === 1 ? '50%' : '8px', 
            cursor: isDragging ? 'grabbing' : 'grab',
            boxShadow: 'inset 0 0 0 1px rgba(0,0,0,0.1)'
          }}
          onMouseDown={handleMouseDown}
          onMouseMove={handleMouseMove}
          onMouseUp={handleMouseUp}
          onMouseLeave={handleMouseUp}
          onTouchStart={handleMouseDown}
          onTouchMove={handleMouseMove}
          onTouchEnd={handleMouseUp}
        >
          {/* 🟢 TỐI ƯU CSS: Render kích thước thật được JS tính toán, lấy lại toàn bộ ảnh gốc */}
          <img 
            ref={imageRef}
            src={imageSrc} 
            alt="crop-preview" 
            draggable={false}
            onLoad={handleImageLoad}
            style={{
              position: 'absolute',
              top: '50%', 
              left: '50%',
              width: imgSize.width ? `${imgSize.width}px` : 'auto', 
              height: imgSize.height ? `${imgSize.height}px` : 'auto',
              transform: `translate(calc(-50% + ${position.x}px), calc(-50% + ${position.y}px)) scale(${zoom})`,
              transformOrigin: 'center',
              pointerEvents: 'none',
              opacity: imgSize.width ? 1 : 0 // Giấu ảnh đi cho đến khi tính toán xong kích thước
            }} 
          />
        </Box>

        <Box sx={{ display: 'flex', alignItems: 'center', width: '100%', maxWidth: 300, mt: 4, gap: 2 }}>
          <ZoomOutIcon color="action" />
          <Slider 
            value={zoom} min={1} max={3} step={0.1}
            onChange={(e, val) => setZoom(val as number)} 
            color="primary"
          />
          <ZoomInIcon color="action" />
        </Box>
      </DialogContent>

      <DialogActions sx={{ p: 2 }}>
        <Button onClick={onClose} color="inherit">Hủy</Button>
        <Button onClick={handleSave} variant="contained" color="primary">Xong</Button>
      </DialogActions>
    </Dialog>
  );
}