import React from 'react';
import { Box, Typography } from '@mui/material';
import AvatarRenderer from '../Cosmetic/AvatarRenderer';
import NameRenderer from '../Cosmetic/NameRenderer';
import type { GachaPhase } from '../../types/gacha';

interface PreviewCombosProps {
  phase: GachaPhase;
  userAvatarUrl?: string;
  userName: string;
  theme: 'SUMMER' | 'WORLDCUP'; // <-- Bổ sung Prop Theme
}

export default function PreviewCombos({ phase, userAvatarUrl, userName, theme }: PreviewCombosProps) {

  // Cấu hình linh hoạt các combo đồ theo Theme
  const config = {
    SUMMER: [
      { frame: 'css-frame-smr-ocean-mythic', color: 'css-color-smr-tsunami', label: 'Combo Đại Dương', labelColor: '#38bdf8', glow: '#0284c7', border: 'rgba(56, 189, 248, 0.3)' },
      { frame: 'css-frame-smr-solar-mythic', color: 'css-color-smr-sun-mythic', label: 'Combo Mặt Trời', labelColor: '#fde047', glow: '#eab308', border: 'rgba(253, 224, 71, 0.5)', isCenter: true },
      { frame: 'css-frame-smr-phoenix-sun', color: 'css-color-smr-sun-god', label: 'Combo Phượng Hoàng', labelColor: '#f87171', glow: '#dc2626', border: 'rgba(248, 113, 113, 0.3)' }
    ],
    WORLDCUP: [
      // Sửa màu tên thành Argentina (Xanh/Trắng/Xanh) cho hợp với tông lạnh của Đèn sân
      { frame: 'css-frame-wc-argentina', color: 'css-color-wc-argentina', label: 'Combo 10h37', labelColor: '#cbd5e1', glow: '#94a3b8', border: 'rgba(203, 213, 225, 0.5)' },

      // Sửa màu tên thành world-champion cho khớp với class Mythic mới
      { frame: 'css-frame-wc-golden-cup', color: 'css-color-wc-world-champion', label: 'Combo Cúp Vàng', labelColor: '#fde047', glow: '#eab308', border: 'rgba(253, 224, 71, 0.6)', isCenter: true },

      // Thay viền lửa chung chung bằng viền Portugal cực xịn
      { frame: 'css-frame-wc-portugal', color: 'css-color-wc-portugal', label: 'Combo 9h53', labelColor: '#fca5a5', glow: '#ef4444', border: 'rgba(239, 68, 68, 0.5)' }
    ]
  }[theme];
  return (
    <Box display="flex" justifyContent="center" alignItems="center" gap={{ xs: 3, sm: 6 }} mb={6} position="relative" zIndex={1} flexWrap="wrap">

      {config.map((combo, index) => (
        <Box
          key={index}
          display="flex"
          flexDirection="column"
          alignItems="center"
          sx={{
            // Nếu là item ở giữa (isCenter), thì scale to lên
            transform: combo.isCenter ? 'scale(1.15)' : 'none',
            zIndex: combo.isCenter ? 2 : 1,
            opacity: (!combo.isCenter && phase === 'ANIMATING') ? 0.5 : 1,
            filter: (combo.isCenter && phase === 'FETCHING') ? 'brightness(1.5)' : 'none',
            transition: '0.3s',
            animation: (combo.isCenter && phase === 'ANIMATING') ? 'pulse-button 0.5s infinite' : 'none'
          }}
        >
          <AvatarRenderer src={userAvatarUrl} effectKey={combo.frame} size={80} />
          <Box
            mt={3}
            bgcolor={combo.isCenter ? "rgba(0,0,0,0.6)" : "rgba(0,0,0,0.5)"}
            px={2} py={0.5}
            borderRadius={5}
            border={`1px solid ${combo.border}`}
          >
            <NameRenderer name={userName} effectKey={combo.color} />
          </Box>
          <Typography
            variant="subtitle2"
            fontWeight="bold"
            color={combo.labelColor}
            mt={1}
            sx={{ textShadow: `0 0 10px ${combo.glow}` }}
          >
            {combo.label}
          </Typography>
        </Box>
      ))}

    </Box>
  );
}