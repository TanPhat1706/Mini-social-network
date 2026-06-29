import React from 'react';
import { Box, Typography, Modal, Backdrop, Fade, Chip, Button } from '@mui/material';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import AvatarRenderer from '../Cosmetic/AvatarRenderer';
import NameRenderer from '../Cosmetic/NameRenderer';
import type { GachaPhase } from '../../types/gacha';
import type { CosmeticItem, CosmeticRarity } from '../../types/cosmetic';
import { RARITY_LABELS } from '../../types/cosmetic';

interface RevealModalProps {
  phase: GachaPhase;
  wonItem: CosmeticItem | null;
  userAvatarUrl?: string;
  userName: string;
  onClose: () => void;
}

const getRarityGlowColor = (rarity: CosmeticRarity) => {
  switch (rarity) {
    case 'MYTHIC': return 'radial-gradient(circle, rgba(236,72,153,0.8) 0%, rgba(0,0,0,0) 70%)';
    case 'LEGENDARY': return 'radial-gradient(circle, rgba(245,158,11,0.8) 0%, rgba(0,0,0,0) 70%)';
    case 'EPIC': return 'radial-gradient(circle, rgba(139,92,246,0.8) 0%, rgba(0,0,0,0) 70%)';
    case 'RARE': return 'radial-gradient(circle, rgba(59,130,246,0.8) 0%, rgba(0,0,0,0) 70%)';
    default: return 'radial-gradient(circle, rgba(156,163,175,0.8) 0%, rgba(0,0,0,0) 70%)';
  }
};

export default function RevealModal({ phase, wonItem, userAvatarUrl, userName, onClose }: RevealModalProps) {
  return (
    <Modal open={phase === 'REVEAL' && wonItem !== null} closeAfterTransition slots={{ backdrop: Backdrop }} slotProps={{ backdrop: { timeout: 500, sx: { backgroundColor: 'rgba(0,0,0,0.85)' } } }}>
      <Fade in={phase === 'REVEAL'}>
        <Box sx={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', width: '100%', maxWidth: 500, textAlign: 'center', outline: 'none' }}>
          {wonItem && (
            <Box className="item-reveal">
              <Box sx={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', width: '200%', height: '200%', zIndex: -1, pointerEvents: 'none', background: getRarityGlowColor(wonItem.rarity as CosmeticRarity), animation: 'glow-spin 10s linear infinite' }} />
              <Typography variant="h4" fontWeight="900" sx={{ mb: 2, color: 'white', textShadow: '0 2px 10px rgba(0,0,0,0.5)' }}>CHÚC MỪNG BẠN NHẬN ĐƯỢC</Typography>
              <Box sx={{ my: 5, display: 'flex', justifyContent: 'center' }}>
                {wonItem.type === 'NAME_COLOR' ? (
                  <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                    <AvatarRenderer src={userAvatarUrl} size={80} />
                    <Typography variant="h4" sx={{ mt: 2 }}><NameRenderer name={userName} effectKey={wonItem.effectKey} /></Typography>
                  </Box>
                ) : (
                  <AvatarRenderer src={userAvatarUrl} effectKey={wonItem.effectKey} size={120} />
                )}
              </Box>
              <Chip label={RARITY_LABELS[wonItem.rarity as CosmeticRarity]} sx={{ mb: 1, fontSize: '18px', fontWeight: 'bold', px: 2, py: 2, bgcolor: 'white', color: 'black' }} icon={<AutoAwesomeIcon />} />
              <Typography variant="h5" fontWeight="bold" sx={{ color: 'white', mb: 4, textShadow: '0 2px 4px rgba(0,0,0,0.8)' }}>{wonItem.name}</Typography>
              <Button variant="contained" color="primary" onClick={onClose} sx={{ borderRadius: 50, px: 5, py: 1.5, fontWeight: 'bold', fontSize: '16px' }}>Đóng & Nhận Vào Kho</Button>
            </Box>
          )}
        </Box>
      </Fade>
    </Modal>
  );
}