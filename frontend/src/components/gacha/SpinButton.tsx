import React from 'react';
import { Box, Button, Typography, CircularProgress } from '@mui/material';
import DiamondIcon from '@mui/icons-material/Diamond';
import type { GachaPhase, GachaInfoResponse } from '../../types/gacha';

interface SpinButtonProps {
  phase: GachaPhase;
  errorMsg: string | null;
  gachaInfo: GachaInfoResponse | null;
  theme: 'SUMMER' | 'WORLDCUP'; // <-- Bổ sung Prop theme
  onSpin: () => void;
}

export default function SpinButton({ phase, errorMsg, gachaInfo, theme, onSpin }: SpinButtonProps) {
  
  // Dynamic Button Colors
  const btnConfig = theme === 'SUMMER' 
    ? { bg: 'linear-gradient(90deg, #f59e0b, #ef4444)', hover: 'linear-gradient(90deg, #fbbf24, #f97316)', shadow: 'rgba(245, 158, 11, 0.6)' }
    : { bg: 'linear-gradient(90deg, #10b981, #047857)', hover: 'linear-gradient(90deg, #34d399, #059669)', shadow: 'rgba(16, 185, 129, 0.6)' };

  return (
    <Box textAlign="center" position="relative" zIndex={1}>
      {errorMsg && (
        <Typography color="#fca5a5" variant="body2" mb={2} fontWeight="bold" sx={{ bgcolor: 'rgba(239, 68, 68, 0.2)', py: 1, px: 2, borderRadius: 2, display: 'inline-block' }}>
          {errorMsg}
        </Typography>
      )}

      <Button
        variant="contained"
        size="large"
        onClick={onSpin}
        disabled={phase !== 'IDLE'}
        sx={{
          borderRadius: 50, px: 6, py: 1.5, fontSize: '18px', fontWeight: '900',
          background: btnConfig.bg,
          boxShadow: `0 0 20px ${btnConfig.shadow}`,
          animation: phase === 'IDLE' ? 'pulse-button 2s infinite' : 'none',
          '&:hover': { background: btnConfig.hover }
        }}
      >
        {phase === 'IDLE' ? (
          <Box display="flex" alignItems="center" gap={1}>
            MỞ RƯƠNG X1 (500 <DiamondIcon fontSize="small" sx={{ color: '#F5A623 !important' }} />)
          </Box>
        ) : (
          <Box display="flex" alignItems="center" gap={1}>
            <CircularProgress size={20} color="inherit" /> ĐANG MỞ...
          </Box>
        )}
      </Button>

      {gachaInfo && (
        <Typography variant="body2" mt={2} color="rgba(255,255,255,0.6)">
          Tiến trình: {gachaInfo.ownedCount} / {gachaInfo.totalPoolSize} vật phẩm.
        </Typography>
      )}
    </Box>
  );
}