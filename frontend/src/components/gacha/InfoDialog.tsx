import React from 'react';
import { Box, Typography, Chip, Dialog, DialogTitle, DialogContent, DialogActions, LinearProgress, Button, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper } from '@mui/material';
import HelpOutlineIcon from '@mui/icons-material/HelpOutline';
import type { GachaInfoResponse } from '../../types/gacha';
import type { CosmeticRarity } from '../../types/cosmetic';
import { RARITY_LABELS } from '../../types/cosmetic';

interface InfoDialogProps {
  open: boolean;
  onClose: () => void;
  gachaInfo: GachaInfoResponse | null;
  theme: 'SUMMER' | 'WORLDCUP'; // <-- Thêm Prop theme
}

export default function InfoDialog({ open, onClose, gachaInfo, theme }: InfoDialogProps) {
  const progressPercentage = gachaInfo ? (gachaInfo.ownedCount / gachaInfo.totalPoolSize) * 100 : 0;

  // Cấu hình DNA thiết kế
  const uiConfig = theme === 'SUMMER'
    ? { iconColor: '#0284c7', progressColor: 'info' as const, tableHeaderBg: '#f0f9ff' } // Tông Xanh biển
    : { iconColor: '#16a34a', progressColor: 'success' as const, tableHeaderBg: '#f0fdf4' }; // Tông Xanh lá sân cỏ

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle sx={{ fontWeight: 'bold', display: 'flex', alignItems: 'center', gap: 1 }}>
        <HelpOutlineIcon sx={{ color: uiConfig.iconColor }} /> Thông tin Rương & Tỷ lệ nổ
      </DialogTitle>
      <DialogContent dividers>
        <Typography variant="body1" fontWeight="bold" sx={{ color: uiConfig.iconColor }} gutterBottom>
          🌟 Cơ chế Rương Hữu Hạn:
        </Typography>
        <Typography variant="body2" mb={3} color="text.secondary">
          Mỗi vật phẩm trong rương chỉ xuất hiện duy nhất 1 lần. Khi bạn quay trúng, vật phẩm đó sẽ bị loại khỏi rương, giúp tăng tỷ lệ nổ các vật phẩm quý hiếm còn lại!
        </Typography>

        {gachaInfo && (
          <Box sx={{ p: 2, bgcolor: uiConfig.tableHeaderBg, borderRadius: 2, mb: 3, border: `1px solid ${uiConfig.iconColor}33` }}>
            <Typography variant="subtitle2" fontWeight="bold" mb={1}>
              Tiến độ thu thập Rương ({gachaInfo.ownedCount} / {gachaInfo.totalPoolSize})
            </Typography>
            {/* Thanh Progress chạy theo màu chủ đề */}
            <LinearProgress 
              variant="determinate" 
              value={progressPercentage} 
              color={uiConfig.progressColor}
              sx={{ height: 10, borderRadius: theme === 'WORLDCUP' ? 1 : 5, mb: 1 }} 
            />
            <Typography variant="body2" color={gachaInfo.remainingForGuarantee === 0 ? "success.main" : "text.secondary"} fontWeight="bold">
              {gachaInfo.remainingForGuarantee === 0 
                ? "🎉 Chúc mừng! Bạn đã thu thập toàn bộ vật phẩm trong rương!" 
                : `Chỉ cần quay ${gachaInfo.remainingForGuarantee} lần nữa là bạn chắc chắn sở hữu 100% phần thưởng!`}
            </Typography>
          </Box>
        )}

        <Typography variant="subtitle1" fontWeight="bold" mb={1}>Bảng Tỷ lệ % hiện tại:</Typography>
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead sx={{ bgcolor: uiConfig.tableHeaderBg }}>
              <TableRow>
                <TableCell><strong>Độ hiếm</strong></TableCell>
                <TableCell align="right"><strong>Tỷ lệ nổ</strong></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {gachaInfo?.currentRates && Object.entries(gachaInfo.currentRates).map(([rarity, rate]) => (
                <TableRow key={rarity}>
                  <TableCell><Chip label={RARITY_LABELS[rarity as CosmeticRarity]} size="small" variant="outlined" sx={{ fontWeight: 'bold' }}/></TableCell>
                  <TableCell align="right" sx={{ fontWeight: 'bold', color: rarity === 'MYTHIC' || rarity === 'LEGENDARY' ? '#d97706' : 'inherit' }}>
                    {rate}%
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} variant="contained" sx={{ borderRadius: 50, bgcolor: uiConfig.iconColor, '&:hover': { filter: 'brightness(0.9)' } }}>
          Đã hiểu
        </Button>
      </DialogActions>
    </Dialog>
  );
}