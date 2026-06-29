import React from 'react';
import { Box, Typography, Chip, Stack } from '@mui/material';
import DiamondIcon from '@mui/icons-material/Diamond';
import InventoryIcon from '@mui/icons-material/Inventory';
import SecurityIcon from '@mui/icons-material/Security';
import EmojiEventsIcon from '@mui/icons-material/EmojiEvents';
import LightModeIcon from '@mui/icons-material/LightMode';
import SportsSoccerIcon from '@mui/icons-material/SportsSoccer';

import '../../styles/banner.css';

interface EventBannerProps {
  theme: 'SUMMER' | 'WORLDCUP';
  ownedCount: number;
  totalPoolSize: number;
}

export default function EventBanner({ theme, ownedCount, totalPoolSize }: EventBannerProps) {
  const config = {
    SUMMER: {
      badgeText: "SUMMER 2026 EVENT",
      icon: <LightModeIcon />,
      title: "Kho Báu Mùa Hè",
      subtitle: "Summer Treasure Collection",
      desc: "Thu thập những Viền Avatar và Màu tên giới hạn cực kỳ rực rỡ. Sự kiện chỉ diễn ra duy nhất trong mùa hè này!",
      colors: { text: '#fbbf24', sub: '#38bdf8', badge: 'warning' as const },
      bgStyle: { background: 'linear-gradient(135deg, #0f172a 0%, #1e3a8a 50%, #0f172a 100%)' }
    },
    WORLDCUP: {
      badgeText: "🏆 OFFICIAL EVENT",
      icon: <EmojiEventsIcon />,
      title: "ĐƯỜNG TỚI VINH QUANG",
      subtitle: "Limited World Cup Cosmetics",
      desc: "Hòa mình vào đêm chung kết rực lửa! Thu thập bộ sưu tập giới hạn chỉ xuất hiện trong mùa World Cup.",
      colors: { text: '#fde047', sub: '#ffffff', badge: 'success' as const },
      bgStyle: { background: 'radial-gradient(circle at center, #0f5132 0%, #071a0d 70%)' }
    }
  }[theme];

  return (
    <Box
      sx={{
        position: 'relative', width: '100%', textAlign: 'center', mb: 6,
        borderRadius: '16px 16px 0 0', overflow: 'hidden', minHeight: '320px', display: 'flex', flexDirection: 'column', justifyContent: 'center',
        ...config.bgStyle
      }}
    >
      {/* LAYER 1: BACKGROUND (Đã code ở CSS) */}
      {theme === 'WORLDCUP' && <Box className="banner-grass" />}

      {/* LAYER 2: HERO OBJECTS */}
      {theme === 'SUMMER' && <Box className="banner-hero-object hero-summer">🌴</Box>}
      {theme === 'WORLDCUP' && <Box className="banner-hero-object hero-wc">🏆</Box>}

      {/* LAYER 3 & 4: PARTICLES & LIGHTS */}
      {theme === 'SUMMER' && (
        <>
          <Box className="banner-sun" />
          <Box className="banner-cloud cloud-1" />
          <Box className="banner-cloud cloud-2" />
          <Box className="banner-sparkle" sx={{ top: '30%', left: '25%', animationDelay: '0s' }}>✨</Box>
          <Box className="banner-sparkle" sx={{ top: '15%', right: '35%', animationDelay: '1s' }}>✨</Box>
        </>
      )}
      {theme === 'WORLDCUP' && (
        <>
          <Box className="banner-stadium-light left" />
          <Box className="banner-stadium-light right" />
          <Box className="banner-football">⚽</Box>
          <Box className="banner-flash flash-1" />
          <Box className="banner-flash flash-2" />
        </>
      )}

      {/* DẢI SÁNG ĐÁY KẾT NỐI BỐ CỤC */}
      <Box className={`banner-bottom-glow ${theme === 'SUMMER' ? 'glow-summer' : 'glow-wc'}`} />

      {/* LAYER 5: CONTENT (Chuẩn hóa layout Top-Middle-Bottom) */}
      <Box position="relative" zIndex={10} pt={4} pb={4} display="flex" flexDirection="column" alignItems="center" flexGrow={1}>

        {/* TOP AREA (20%) */}
        <Box mb={2} minHeight="40px">
          {theme === 'WORLDCUP' ? (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, bgcolor: 'rgba(0,0,0,0.6)', px: 3, py: 0.5, borderRadius: 1, border: '1px solid rgba(255,255,255,0.1)' }}>
              <Typography fontWeight="bold" color="white" fontSize="14px">FINAL</Typography>
              <Box sx={{ bgcolor: '#000', px: 2, py: 0.5, borderRadius: 1, color: '#fde047', fontWeight: 'bold' }}>[ 90' ]</Box>
              <Typography fontWeight="bold" color="white" fontSize="14px">MATCH</Typography>
            </Box>
          ) : (
            <Chip label={config.badgeText} color={config.colors.badge} icon={config.icon} sx={{ fontWeight: 'bold', letterSpacing: 1, boxShadow: '0 0 10px rgba(0,0,0,0.5)' }} />
          )}
        </Box>

        {/* MIDDLE AREA (60%) */}
        <Box flexGrow={1} display="flex" flexDirection="column" justifyContent="center">
          {theme === 'WORLDCUP' && <Chip label={config.badgeText} size="small" color={config.colors.badge} sx={{ mb: 1, alignSelf: 'center' }} />}
          <Typography variant="h3" fontWeight="900" sx={{ textTransform: 'uppercase', letterSpacing: 2, color: config.colors.text, textShadow: `0 4px 20px rgba(0,0,0,0.8)`, mb: 1 }}>
            {config.title}
          </Typography>
          <Typography variant="h6" color={config.colors.sub} fontWeight="bold" sx={{ letterSpacing: 1, mb: 1 }}>
            {config.subtitle}
          </Typography>
          <Typography variant="body1" color="rgba(255,255,255,0.8)" maxWidth={600} mx="auto" sx={{ textShadow: '0 1px 3px black' }}>
            {config.desc}
          </Typography>
        </Box>

        {/* BOTTOM AREA (20%) */}
        <Box mt={3} width="100%">
          <Box
            display="flex"
            flexWrap="wrap"
            justifyContent="center"
            gap={2}
            px={2} /* Thêm padding hai bên để không bao giờ chạm sát mép màn hình */
          >
            <Chip
              icon={theme === 'WORLDCUP' ? <SportsSoccerIcon fontSize="small" sx={{ color: '#fff !important' }} /> : <InventoryIcon fontSize="small" sx={{ color: '#22d3ee !important' }} />}
              label={`Đã sưu tầm: ${ownedCount} / ${totalPoolSize}`}
              variant="outlined"
              sx={{ color: '#fff', borderColor: 'rgba(255, 255, 255, 0.3)', bgcolor: 'rgba(0,0,0,0.6)' }}
            />
            <Chip
              icon={<DiamondIcon fontSize="small" sx={{ color: '#F5A623 !important' }} />}
              label="10 Điểm / Lượt"
              variant="outlined"
              sx={{ color: '#fff', borderColor: 'rgba(255,255,255,0.3)', bgcolor: 'rgba(0,0,0,0.6)' }}
            />
            <Chip
              icon={theme === 'WORLDCUP' ? <EmojiEventsIcon fontSize="small" sx={{ color: '#fde047 !important' }} /> : <SecurityIcon fontSize="small" color="success" />}
              label={theme === 'WORLDCUP' ? "Limited Edition" : "Bảo hiểm: Không trùng lặp"}
              variant="outlined"
              sx={{ color: '#fff', borderColor: 'rgba(255,255,255,0.3)', bgcolor: 'rgba(0,0,0,0.6)' }}
            />
          </Box>
        </Box>

      </Box>
    </Box>
  );
}